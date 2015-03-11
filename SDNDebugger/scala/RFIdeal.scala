import net.sdn.event._
import net.sdn.policy.Policy
import com.google.gson.Gson
import scala.util.control.Breaks._
import rx.lang.scala.Observable

class RFIdeal(fwswitchid: String) {
	var policyList: List[Policy] = List()

	def isInInt(e: NetworkEvent): Boolean = {
		e.direction == NetworkEventDirection.IN
	}

	def isOutSame(orig: NetworkEvent): NetworkEvent => Boolean = {
		{e => {
			e.pkt.eth.ip.nw_src == orig.pkt.eth.ip.nw_src &&
			e.pkt.eth.ip.nw_dst == orig.pkt.eth.ip.nw_dst &&
			e.sw == orig.sw && e.pkt.eth.ip.icmp.sequence == orig.pkt.eth.ip.icmp.sequence &&
			e.direction == NetworkEventDirection.OUT
			}
		}
	}

	def isMatchPolicyAllow(e: NetworkEvent): Boolean = {
		var ret = false
		breakable {
			for (p <- policyList) {
				if (p.isMatched(e.pkt)) {
					// default empty string is "ALLOW"
					if (p.actions != "DENY") {
						ret = true
					}
					println(p.actions)
					break
				}
			}
		}

		ret
	}

	def isMatchPolicyDeny(e: NetworkEvent): Boolean = {
		// no policy means deny
		var ret = true
		breakable {
			for (p <- policyList) {
				if (p.isMatched(e.pkt)) {
					// default empty string is "ALLOW"
					if (p.actions != "DENY") {
						ret = false
					}
					break
				}
			}
		}

		ret
	}

	def addPolicy(e: NetworkEvent) {
		val p = new Gson().fromJson(new String(e.pkt.eth.ip.tcp.payload), classOf[Policy])
		policyList = insert(p, policyList)
	}

	def insert(p: Policy, l: List[Policy]): List[Policy] = {
		if (l.isEmpty || l.head.priority >= p.priority) {
			p :: l
		} else {
			l.head :: insert(p, l.tail)
		}
	}

	val ICMPStream = Simon.nwEvents().filter(SimonHelper.isICMPNetworkEvents)
	val RESTStream = Simon.nwEvents().filter(SimonHelper.isRESTNetworkEvents)

	RESTStream.subscribe(addPolicy(_))

	val e1 = ICMPStream.filter(isInInt).filter(isMatchPolicyAllow).map(e =>
				Simon.expect(ICMPStream, isOutSame(e), Duration(100, "milliseconds")).first
				match {
					// check again for immediately updating rule
					case eviol: ExpectViolation => if (isMatchPolicyAllow(e)) new ExpectSuccess() else eviol
				 	case _ => new ExpectSuccess()
				});

	val e2 = ICMPStream.filter(isInInt).filter(isMatchPolicyDeny).map(e =>
				Simon.expectNot(ICMPStream, isOutSame(e), Duration(100, "milliseconds")).first
				match {
					// check again for immediately updating rule
					case eviol: ExpectViolation => if (isMatchPolicyAllow(e)) new ExpectSuccess() else eviol
				 	case _ => new ExpectSuccess()
				});

	val violations = e1.merge(e2)
	
	val autosubscribe = violations.subscribe(Simon.printwarning(_))
}
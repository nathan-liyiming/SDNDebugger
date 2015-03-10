import net.sdn.event._
import net.sdn.policy.Policy
import com.google.gson.Gson
import scala.util.control.Breaks._
import rx.lang.scala.Observable

class RFIdeal(fwswitchid: String) {
	var policyList: List[Policy] = List()

	def isComingIn(e: NetworkEvent): Boolean = {
		e.direction == NetworkEventDirection.IN
	}

	def isPassThrough(orig: NetworkEvent): NetworkEvent => Boolean = {
		{e => {
			e.pkt.eth.ip.nw_src == orig.pkt.eth.ip.nw_src &&
			e.pkt.eth.ip.nw_dst == orig.pkt.eth.ip.nw_dst &&
			e.sw == orig.sw && e.direction == NetworkEventDirection.OUT
			}
		}
	}

	def isMatchPolicyAllow(e: NetworkEvent): Boolean = {
		var ret = false
		breakable {
			for (p <- policyList) {
				if (p.isMatched(e.pkt)) {
					if (p.actions == "ALLOW") {
						ret = true
					}
					break
				}
			}
		}

		ret
	}

	def isMatchPolicyDeny(e: NetworkEvent): Boolean = {
		var ret = false
		breakable {
			for (p <- policyList) {
				if (p.isMatched(e.pkt)) {
					if (p.actions == "DENY") {
						ret = true
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

	val e1 = ICMPStream.filter(isComingIn).filter(isMatchPolicyAllow).map(e =>
				Simon.expect(ICMPStream, isPassThrough(e), Duration(100, "milliseconds")).first
				match {
					// check again for immediately updating rule
					case eviol: ExpectViolation => if (isMatchPolicyAllow(e)) new ExpectSuccess() else eviol
				 	case _ => new ExpectSuccess()
				});

	val e2 = ICMPStream.filter(isComingIn).filter(isMatchPolicyDeny).map(e =>
				Simon.expectNot(ICMPStream, isPassThrough(e), Duration(100, "milliseconds")).first
				match {
					// check again for immediately updating rule
					case eviol: ExpectViolation => if (isMatchPolicyAllow(e)) new ExpectSuccess() else eviol
				 	case _ => new ExpectSuccess()
				});	

	val violations = e1.merge(e2)
	
	val autosubscribe = violations.subscribe(Simon.printwarning(_))
}
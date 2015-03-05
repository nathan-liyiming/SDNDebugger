/*
	Ideal model + reactor for Stateful Firewall example


	Could be enhanced to use cancellable expectations; right now this model
	will be confused by an over-eager external host.  Consider
		(1) ext -> [expect not to see corresponding output]
		(2) int -> [expect to pass, modify state;cancel original expectation (assuming no way to distinguish (1) and (3))]
		(3) ext -> [expect to pass]

*/

/*
	With respect to a topology that defines a set of SFW switches and, for each switch, a set of internal and external ports...
		ext -> int [not in state] => expect dropped
		int -> ext => expect pass, add to state
		ext -> int [in state] => expect pass
*/

import net.sdn.phytopo.Host;
import net.sdn.phytopo.PhyTopo;
import net.sdn.phytopo.Switch;

class SFIdeal(topofn: String) {
	def topo = new PhyTopo(topofn);
}
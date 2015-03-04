/*
	Ideal model + reactor for Stateful Firewall example

	
	Could be enhanced to use cancellable expectations; right now this model
	will be confused by an over-eager external host.  Consider 
		(1) ext -> [expect not to see corresponding output]
		(2) int -> [expect to pass, modify state;cancel original expectation (assuming no way to distinguish (1) and (3))] 
		(3) ext -> [expect to pass]

*/

object SFIdeal {
	

}
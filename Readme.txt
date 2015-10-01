=431 Project=
==Election Selection==

==Group===
Richard Gerdes
Sangini Shah
Corey Wu

==Protocol==
===Monitor===
1) Wait for peers to connect
	- notify peers of new peer
2) Notify peers that vote is starting with count of peers (N)
3) wait for peer to reach N/2+ votes
4) Notify peers that vote is complete
5) Wait for all peers to report
6) Display count

===Passing Votes===
1) Each Peer has a list of others
2) Each Peer starts with 1 vote -> VOTES
   0 messages passed -> M
3) Start with the First peer In the List
	i)   Generate a number of votes to pass (X) between 1 and VOTES
	ii)  Offer peer X votes (M++)
		> Accept
			- subtact X from VOTES
				VOTES > 0 => repeat 3
				ELSE => stop. goto 4
		> Reject Busy
			- try next peer (repeat 2)
		> Reject Passed
			- Remove peer from list.
			- Get peer remote passed votes to
			- Repeat ii with new peer
4) Wait for notification of election complete from Monitor
5) Pass message counts to Moniter

===Recieving Votes===
//remote peer offers n votes to node (OFFER)
if(VOTES > 0)
	if(currentlyPassing)
		> Reject Busy (M++)
	else
		> Accept (M++)
			- added OFFER to VOTES
			- if VOTES > N / 2
				> Notify Monitor ELECTION COMPLETE
else
	> Reject Passed (M++)
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import cern.colt.bitvector.BitVector;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import java.util.Arrays;
import jade.core.AID;
import java.util.Random;

public class PeerNode extends Agent {
	
	private int chunksNumberN, chunkSizeX, peersNum;
	private short thresholdT;
	private HashMap<BitVector, int[]> hmap;

	//launch agent automatically
	protected void setup() {
		System.out.println("Peer Agent "+getAID().getName()+" is ready to create peer agents");
		
		//get arguments from helper node
		Object[] args = getArguments();
		if (args !=null && args.length >= 0) {
			   peersNum = Integer.parseInt((String) args[0]);
			   chunksNumberN = Integer.parseInt((String) args[1]);
			   chunkSizeX = Integer.parseInt((String) args[2]);
			   thresholdT = Short.parseShort((String) args[3]);
	    }
		hmap = new HashMap<BitVector, int[]>(chunksNumberN);
		
		//create N storage locations
		for (int i=0; i<chunksNumberN; i++) {
			BitVector randAddresses = new BitVector(chunkSizeX);
			int counters[]= new int[chunkSizeX];
			
			//generate random addresses and 0 counters
			for (int j = 0; j < chunkSizeX; j++) {
				randAddresses.putQuick(j, Math.random() > .5);
				counters[j]=0;
			}
			//put generated data into hashmap
			hmap.put(randAddresses, counters);	
		}

		// Register the peer agent service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("peerAgent");
		sd.setName(getLocalName()+"-Peer Agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		addBehaviour(new ReceiveMessage());	
	}
	
	// called to delete the agent
	protected void takeDown() {
			// Deregister agent from the Directory Facilitator 
			try {
				DFService.deregister(this);
				System.out.println("Peer-agent "+getAID().getName()+" has been signed off.");
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
			// Printout a dismissal message
			System.out.println("Peer-agent "+getAID().getName()+"terminated.");
	}

	public int[] searchData(String data) {
		BitVector bitString = new BitVector(data.length());
		int[] sumResult = new int[bitString.size()]; // -> to send back  - sum of all values of counters within threshold
		
		// iterate through hashmap -> 
		Iterator<Map.Entry<BitVector, int[]>> it = hmap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<BitVector,int[]> pairs = it.next();
			BitVector currentAddress = (BitVector)pairs.getKey();
			int[] countersArray;
			
			if(thresholdT >= hammingDistance(bitString, currentAddress)) {
				countersArray = (int[])pairs.getValue();
				for(int i=0; i<bitString.size(); i++ ) {
					sumResult[i] += countersArray[i];
				}
			}
		}
		return sumResult;
	}
	
	public int storeData(String data) {
		//System.out.println("Data received: "+data);
		BitVector datatoStore = new BitVector(data.length());
		int storedChunksCounter = 0;
		
		for(int i=0; i< data.length(); i++) {
			if(data.charAt(i) == '1') {
				datatoStore.set(i); 
			}
		}
		
		// loop through the hashmap 
		Iterator<Map.Entry<BitVector, int[]>> it = hmap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<BitVector, int[]> pairs = it.next();
			int placeHolder[] = (int[])pairs.getValue();
			
			// store the data if the distance is suitable
			if(hammingDistance(datatoStore, (BitVector)pairs.getKey())<= thresholdT) {
				for(int i=0; i<datatoStore.size(); i++ ) {
					if(datatoStore.get(i)==true) {
						placeHolder[i]++;
					}
					else {
						placeHolder[i]--;
					}
				}
				pairs.setValue(placeHolder);
				storedChunksCounter++;
				
			}
		}
		// return number of stored chunks
		return storedChunksCounter;
	}

	public String getBitVectorStr(BitVector v) {
		String s="";
		for (int i= 0; i < v.size(); i++) {
			if (v.get(i)) 
				s+="1";                
			else 
				s+="0";  
		}
		return s;
	}

	public int hammingDistance(BitVector toStore, BitVector address) {
		BitVector v1 = toStore.copy();
		BitVector v2 = address.copy();
		v1.xor(v2);	
		return  v1.cardinality();
	}
	
	public class ReceiveMessage extends CyclicBehaviour {

		public void action() {
			int[] searchResult = new int[chunksNumberN];
			int storedChunksCounter;

			//System.out.println(msg.getPerformative());

			// block until message is received...
			ACLMessage msg = receive();
			if (msg != null) {
				String content = msg.getContent();
				// storedChunksCounter=0;
				// if REQUEST type, then we received a command to store...
				if(msg.getPerformative() == ACLMessage.REQUEST) {
					// execute store and record in how many chunks it was stored successfully
					storedChunksCounter = storeData(content);
					// return successfully stored chunks number
					ACLMessage replyToStoreRequest = createMessage(ACLMessage.CONFIRM, storedChunksCounter+"", msg.getSender());
					send(replyToStoreRequest);
				}
				// if PROPOSE type, then execute search on the proposed bitstring...
				else if (msg.getPerformative() == ACLMessage.PROPOSE) {
					//System.out.println("\n\nBitstring to search for in peer's address space: \n" + content);
					// collect summed search results in int array
					searchResult = searchData(content);
					// send back collected int array to requester peer
					ACLMessage replyToSearchQuery = createMessage(ACLMessage.INFORM, Arrays.toString(searchResult), msg.getSender());
					send(replyToSearchQuery);
				}
				else if (msg.getPerformative() == ACLMessage.CANCEL) {
					//System.out.println("Peer node received request to clear ");
					
					Iterator<Map.Entry<BitVector, int[]>> it = hmap.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry<BitVector, int[]> pairs = it.next();
						int placeHolder[] = (int[])pairs.getValue();

						for(int i=0; i<pairs.getKey().size(); i++ ) {
								placeHolder[i]=0;
						}
						pairs.setValue(placeHolder);
					}
				} else if(msg.getPerformative() == ACLMessage.FAILURE) {
					doDelete();
				}
			} else {
				block();
			}
		}
		
		// create message to send to peers
		private ACLMessage createMessage (int mp, String content, AID dest) {
			ACLMessage msgACL;
			msgACL = new ACLMessage(mp);
			msgACL.setContent(content);
			msgACL.addReceiver(dest);
			return msgACL;
		}
	}
}
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import javax.swing.JOptionPane;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import cern.colt.bitvector.BitVector;
import java.io.*;
import java.util.Arrays;

public class PeerNodeWithGUI extends Agent{
	
	private PeerNodeGUI peerNodeGUI;
	
	// standard parameters to start the peer
	private int chunksNumberN, chunkSizeX, peersNum;
	private short thresholdT;

	// hmap holds the entries in the SDM of this local node, but the GUI node does not use it yet...
	private HashMap<BitVector, int[]> hmap;

	// genData holds the bits we want to search in the SDM
	private BitVector genData;
	
	// string version of the genData bitvector
	private String genDataString="";

	// global counter to measure iteration count...
	private int searchIterateCounter=0;

	// timers for measuring how long it takes to do stuff
	long startTime;
	long stopTime;


	

	protected void setup() {
		System.out.println("Peer Agent with GUI"+getAID().getName()+" is ready");
		peerNodeGUI = new PeerNodeGUI(this);
		peerNodeGUI.showGUI();

		Object[] args = getArguments();
		if (args !=null && args.length >= 0) {
		   peersNum = Integer.parseInt((String) args[0]);
		   chunksNumberN = Integer.parseInt((String) args[1]);
		   chunkSizeX = Integer.parseInt((String) args[2]);
		   thresholdT = Short.parseShort((String) args[3]);
		}
		
		hmap = new HashMap<BitVector, int[]>(chunksNumberN);

		// create BitVector with correct chunksize
		genData = new BitVector(chunkSizeX);

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
		sd.setName(getLocalName()+"-Peer Agent with GUI");
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
			if (peerNodeGUI != null) { // Dispose the GUI if it is there
				peerNodeGUI.dispose();
			}
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

	// generates random data and puts it into the text field and the global variables that store it (genData and genDataString)...
	public void generateData() {
		// clear the global string for any residual remains...
		genDataString="";
		
		//generate data chunk
		for (int i = 0; i < chunkSizeX; i++) {
			genData.putQuick(i, Math.random() > .5);
			if (genData.get(i)) 
				genDataString+="1";                
		      else 
		    	genDataString+="0"; 
		} 
		// update the textfield with the created random bitstring...
		peerNodeGUI.dataTF.setText(genDataString);
	}

	public void updateGenData(String newBitString) {
		for (int i = 0; i < genData.size(); i++) {
			if (newBitString.charAt(i) == '1') 
				genData.set(i);                
		      else 
		    	genData.clear(i); 
		}
	}

	// prints the genData BitVector to the standard output...
	public void printgenDataBitVector() {
		System.out.println("genDataBitVector");
		for (int i=0; i<genData.size(); i++) {
			if (genData.get(i)==false) {
				System.out.print("0");
			}else {
				System.out.print("1");
			}
		}
		System.out.println();
	}
	
	public void searchData() {
		// don't proceed if the text-field is not properly filled out...
		if (peerNodeGUI.dataTF.getText().length() != chunkSizeX) {
			peerNodeGUI.logTA.append("Invalid search string, please remember the X (chunkSize) ...");
			return;
		}
		// prepare variables for searching...
		genDataString = peerNodeGUI.dataTF.getText();
		// this will store the string from txt field into the genData bitVector variable...
		updateGenData(genDataString);
		//peerNodeGUI.logTA.append("genDataString:\n"+genDataString);
		//printgenDataBitVector();


		peerNodeGUI.logTA.append("\n=== SEARCH STARTING ===");
		// create one-shot behaviour to send all messages to all peers to initiate searching...
		addBehaviour(new OneShotBehaviour() {
			@Override
			public void action() {
				// reset ccounter which is used to limit number of iterations...
				searchIterateCounter=0;

				// if the bitvector is not empty (for some reason it could be? :D)
				if (0 != genData.size()) {

					// prepare message object
					ACLMessage msgACL = new ACLMessage(ACLMessage.PROPOSE);
					msgACL.setContent(genDataString);
					// iterate to add all receivers
					for (int i=1; i<peersNum; i++)
					{
						msgACL.addReceiver(new AID("PeerAgent" + (peersNum-i), AID.ISLOCALNAME));
					}
					// once iteration is over, send the message 
					send(msgACL);

					startTime = System.nanoTime();

					// and display a message to confirm
					peerNodeGUI.logTA.append("\nBroadcasting search request to all peers:");
				} 
				else {
					// pup up an error if genData is empty... (shouldn't happen but still...)
					JOptionPane.showMessageDialog(peerNodeGUI, "No data to store!", "WARNING", JOptionPane.WARNING_MESSAGE);
					return;
				}
			}
		});
	}
	
	// function called by the GUI to store data from the text field...
	public void storeData() {
		// don't proceed if the text-field is not properly filled out...
		if (peerNodeGUI.dataTF.getText().length() != chunkSizeX) {
			peerNodeGUI.logTA.append("Invalid data cannot be stored! Please click generate or specify data with correct length...");
			return;
		}
		// prepare variables for searching...
		genDataString = peerNodeGUI.dataTF.getText();
		// this will store the string from txt field into the genData bitVector variable...
		updateGenData(genDataString);
		//peerNodeGUI.logTA.append("genDataString:\n"+genDataString);
		//printgenDataBitVector();

		peerNodeGUI.logTA.append("\n=== STORAGE REQUESTED ===");
		addBehaviour(new OneShotBehaviour() {
			@Override
			public void action() {
				// if bitVector holding our storage string is not empty them broadcast it to all peers...
				if (0 != genData.size()) {

					// prepare new message, just one, with all peers as receivers...
					ACLMessage msgACL = new ACLMessage(ACLMessage.REQUEST);
					msgACL.setContent(genDataString);
					// iterate to add all receivers...
					for (int i=1; i<peersNum; i++) {
						msgACL.addReceiver(new AID("PeerAgent" + (peersNum-i), AID.ISLOCALNAME));
					}
					// send message and update the gui
					send(msgACL);

					startTime = System.nanoTime();

					peerNodeGUI.logTA.append("\nBroadcasting data to store:\n"+genDataString+"\n");
				} 
				else {
					// display error popup if data is not correct in bitvector...
					JOptionPane.showMessageDialog(peerNodeGUI, "No data to store!", "WARNING", JOptionPane.WARNING_MESSAGE);
				}
			}
		});	
	}
	

	public void clearData() {
		peerNodeGUI.logTA.append("\n=== CLEANING SDM Peers ===");
		addBehaviour(new OneShotBehaviour() {
			@Override
			public void action() {
				ACLMessage msgACL = new ACLMessage(ACLMessage.CANCEL);
				msgACL.setContent("cleaning");
				for (int i=1; i<peersNum; i++) {
					msgACL.addReceiver(new AID("PeerAgent" + (peersNum-i), AID.ISLOCALNAME));
				}
				send(msgACL);
			}
		});	
	}

	public class ReceiveMessage extends CyclicBehaviour {
		// for coordinating steps of the communication flow...
		private int step = 0;

		// for counting how many replies have been received in certain parts of the communication flow
		private int repliesCounter = 0;	

		// for storing the parsed integers from the msg content string
		private int[] replyIntArray = new int[chunkSizeX];

		// for storing the summed up values for all returned vectors from all peers
		private int[] sumVector = new int[chunkSizeX];

		// for holding the number of chunks that were successfully stored in a single request...
		private int storedChunksCounter;
		
		public void action() {
			//bitvector storing summed vector in binary form...
			BitVector binary = new BitVector(chunkSizeX);
			BitVector bitStrSearch = genData.copy();
	
			switch (step) {
				// STEP 1: Collect all messages (both store and search requests)
				case 0:
					ACLMessage msg = receive();
					if (msg != null) {
						String content = msg.getContent();
						//System.out.println("Peer["+repliesCounter+"] sent:\t"+content);
						
						// if message is INFORM it meas that search results are coming in...
						if (msg.getPerformative() == ACLMessage.INFORM) {
							
							replyIntArray = convertStringToArray(content);
							//System.out.println("SumVector:");
							for (int i=0; i<sumVector.length; i++) {
								sumVector[i] += replyIntArray[i];
								//System.out.print(sumVector[i]+" ");
							}
							//System.out.println();
							repliesCounter++;
							// this triggers the processing of sumvector after all replies have been received for SEARCH request...
							if (repliesCounter >= peersNum-1) {
								step = 1;
							}
							//System.out.println("\nProgress to step 1");
							
						// otherwise if type is CONFIRM it means that storage confirmatoin is coming in...	
						} else if (msg.getPerformative() == ACLMessage.CONFIRM)
						{
							// increase the replies counter to reflect the message currently being processed
							repliesCounter++;
							// add the number of stored chunks to the accumulating variable
							storedChunksCounter += Integer.parseInt(content);
							// this triggers if we have received all replies
							if (repliesCounter >= peersNum-1) 
							{
								// reset counters
								stopTime = System.nanoTime();

								peerNodeGUI.logTA.append("Successfully stored ["+storedChunksCounter+"/"+(peersNum-1)*chunksNumberN+"] HardLocations in "+(stopTime-startTime)/1000000+" ms.\n");
								step = 0;
								repliesCounter = 0;
								storedChunksCounter = 0;
								// update GUI
							}
						} else if(msg.getPerformative() == ACLMessage.FAILURE) {
							doDelete();
						} 
					} else {
						block();
					}
					break;
				// if search request was made proceed to iterate the search if needed...
				case 1:
					// convert sumvector to binary format:
					for (int i=0; i< sumVector.length; i++) {
						if (sumVector[i] > 0) {
							binary.set(i);
						} else{
							binary.clear(i);
						}
					}
					//System.out.println("SUMVECTOR in binary: ");
					//System.out.println(getBitVectorStr(binary));
					
					//System.out.print("\nHalf of Chunksize is "+chunkSizeX/2+" HammingDist is: " + hammingDistance(binary, bitStrSearch));
					//System.out.print("\nHammingDist1 is: " + hammingDistance(binary, bitStrSearch)+" HammingDist2 is: " + hammingDistance(binary, bitStrSearch));

					// if distance is between 0 and chunksizeX/2 then iterate, but not more than the iteratecounter threshold... 
					if ((hammingDistance(binary, bitStrSearch) <= (chunkSizeX/2)) && (hammingDistance(binary, bitStrSearch)>0) && (searchIterateCounter < 50) ) {
						// display a "." in the GUI text area to show iteration progress...
						peerNodeGUI.logTA.append(".");

						// sent msg to all peers
						ACLMessage msgACL = new ACLMessage(ACLMessage.PROPOSE);
						msgACL.setContent(getBitVectorStr(binary));
						for (int i=1; i<peersNum; i++) {
							msgACL.addReceiver(new AID("PeerAgent" + (peersNum-i), AID.ISLOCALNAME));	
						}
						send(msgACL);

						// update the searchstring for the next iteration!
						bitStrSearch = binary.copy();
						searchIterateCounter++;
					}
					// if the hamming distance is 0, that means we found it in the SDM
					else if(hammingDistance(binary, bitStrSearch) == 0) {
						
						peerNodeGUI.logTA.append("\n    ██████╗   ██╗  ██╗\n");
						peerNodeGUI.logTA.append("   ██╔═══██╗  ██║ ██╔╝\n");
						peerNodeGUI.logTA.append("   ██║     ██║  █████╔╝\n");
						peerNodeGUI.logTA.append("   ██║     ██║  ██╔═██╗\n");
						peerNodeGUI.logTA.append("   ╚██████╔╝ ██║  ██╗\n");
						peerNodeGUI.logTA.append("    ╚═════╝       ╚═╝  ╚═╝\n");
										 
						stopTime = System.nanoTime();
						peerNodeGUI.logTA.append("(btw, it took "+(stopTime-startTime)/1000000+" ms to find it)\n");

										 
						bitStrSearch = genData.copy();
					}
					// otherwise we could not find it no matter how many iterations were performed...
					else {
						peerNodeGUI.logTA.append("\n    ███       ██╗    ████╗                ██╗\n");
						peerNodeGUI.logTA.append("    ████     ██║  ██╔══██╗    ██╗ ██║\n");
						peerNodeGUI.logTA.append("    ██╔██    ██║ ██║   ██║      ╚═╝██║ \n");
						peerNodeGUI.logTA.append("    ██║╚██  ██║ ██║   ██║     ██╗ ██║ \n");
						peerNodeGUI.logTA.append("    ██║ ╚████║  ╚████╔╝       ╚═╝ ╚██╗\n");
						peerNodeGUI.logTA.append("    ╚═╝    ╚════╝    ╚════╝                  ╚═╝\n");				
						stopTime = System.nanoTime();
						peerNodeGUI.logTA.append("(btw, it took "+(stopTime-startTime)/1000000+" ms to search)\n");

																							   
					}
					
					//bitStrSearch = genData.copy;
					// clear sumvector and other counters before returnin to msg receiving mode again to process replies...
					for (int j = 0; j<sumVector.length; j++) {
						sumVector[j] = 0;
					}
					step = 0;
					repliesCounter = 0;
					break;
			}
			
		}

		// get string representation of a bitvector passed as argument
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

		// calculate hamming distance between two vectors
		public int hammingDistance(BitVector toStore, BitVector address) {
			BitVector v1 = toStore.copy();
			BitVector v2 = address.copy();
			v1.xor(v2);	
			return  v1.cardinality();
		}

		// helper function to convert string array to int array
		public int[] convertStringToArray(String text) {
			String cleanTxt = cleanString(text);
			String[] splitStrings = cleanTxt.split(",");
			int counter = 0;
			
			int[] intarray = new int[splitStrings.length];
			int i=0;
			for(String str:splitStrings){
				try {
					intarray[i]=Integer.parseInt(str);
					i++;
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Not a number: " + str + " at index " + i, e);
				}
			}
			return intarray;
			
		}

		// helper function to clean string of "[" "]" " " characters at beginning and end
		public String cleanString(String toClean) {
			String newStr = "";
			for (int i=0; i<toClean.length(); i++) {
				if ((toClean.charAt(i) != '[') && (toClean.charAt(i) != ']') && (toClean.charAt(i) != ' ')){
					newStr += toClean.charAt(i);
				}
			}
			return newStr;
		}
	}
}
import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import jade.core.AID;
import jade.lang.acl.ACLMessage;


public class HelperNode extends Agent {
	
	// variable for storing the global GUI
	private HelperNodeGUI helperNodeGUI;
	private int peersNumber = 0;
	
	//launch agent automatically
	protected void setup() {
		System.out.println("Helper Agent "+getAID().getName()+" is ready to create peer agents");
		 
		helperNodeGUI = new HelperNodeGUI(this);
		helperNodeGUI.showGUI();
		 
		// Register the master agent service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("helperAgent");
		sd.setName(getLocalName()+"-Helper Agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
	}
	
	// called to delete the agent
	protected void takeDown() {
		// Dispose the GUI if it is there
		if (helperNodeGUI != null) { 
			helperNodeGUI.dispose();
		}
		
		// Deregister agent from the Directory Facilitator 
		try {
			DFService.deregister(this);
			System.out.println("Helper-agent "+getAID().getName()+" has been signed off.");
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Printout a dismissal message
		System.out.println("Helper-agent "+getAID().getName()+"terminated.");
	}
	
	
	
	public void CreatePeerAgents(String peersNum,String chunksNum,String chunkSize,String threshold) throws ControllerException {
		
		helperNodeGUI.startButton.setEnabled(false);
		helperNodeGUI.logTA.setText("System started");
		helperNodeGUI.stopButton.setEnabled(true);
	
		jade.core.Runtime runtime = jade.core.Runtime.instance();
		Profile profile = new ProfileImpl();
		profile.setParameter(Profile.MAIN_HOST, "localhost");
		AgentContainer container = runtime.createAgentContainer(profile);
        AgentController ag;
        
        for (int i = 1; i < Integer.parseInt(peersNum); i++) {
			try {
				ag = container.createNewAgent("PeerAgent" + (peersNumber+i), "PeerNode", new Object[] {peersNum, chunksNum, chunkSize, threshold});
				ag.start();
		
			} catch (StaleProxyException ex) {
				ex.printStackTrace();
			}
        }
        
        try {
			ag = container.createNewAgent("PeerAgentWithGUI", "PeerNodeWithGUI", new Object[] {peersNum, chunksNum, chunkSize, threshold});
			ag.start();
	
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
       
        peersNumber += Integer.parseInt(peersNum);
	}


	public void stopSystem() {
		ACLMessage msgACL = new ACLMessage(ACLMessage.FAILURE);
		// broadcast messages to all peers to shut down gracefully
		for (int i = 1; i < peersNumber; i++) {
			msgACL.addReceiver(new AID("PeerAgent" + i, AID.ISLOCALNAME));
		}
		// send the shutdown to the peer with GUI as well
		msgACL.addReceiver(new AID("PeerAgentWithGUI", AID.ISLOCALNAME));
		send(msgACL);

		// fix buttons
		helperNodeGUI.stopButton.setEnabled(false);
		helperNodeGUI.logTA.setText("System stopped");
		helperNodeGUI.startButton.setEnabled(true);

		peersNumber = 0;
	}
}

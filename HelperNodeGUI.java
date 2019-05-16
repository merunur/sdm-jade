import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import jade.wrapper.ControllerException;

public class HelperNodeGUI extends JFrame{
	
		private static final long serialVersionUID = 1L;

		private HelperNode helperAgent;
		
		JPanel mainPanel, peersPanel, chunksPanel, bitsPanel, thresholdPanel, buttonsPanel;
		JLabel peersLabel, chunksLabel, bitsLabel, thresholdLabel;
		JTextField peersTF, chunksTF, bitsTF, thresholdTF;
		JButton startButton, stopButton;
		JTextArea logTA, searchLogTA;
		
		public HelperNodeGUI(HelperNode myAgent) {
			super(myAgent.getLocalName());
			
			helperAgent = myAgent;
			
			mainPanel = new JPanel();
			BoxLayout boxlayoutY = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
			mainPanel.setLayout(boxlayoutY);
			
			// specify number of peers to create ---- P
			peersPanel = new JPanel();
			BoxLayout boxlayoutX = new BoxLayout(peersPanel, BoxLayout.X_AXIS);
			peersPanel.setLayout(boxlayoutX);
			
			peersLabel = new JLabel("Number of peers [P]: ");
			peersLabel.setPreferredSize(new Dimension(200, 30));
			
			peersTF = new JTextField("20");
			peersTF.setPreferredSize(new Dimension(200, 30));
			
			peersPanel.add(peersLabel);
			peersPanel.add(peersTF);
			
			//number of chunks one peer can store ---- N
			chunksPanel = new JPanel();
			BoxLayout boxlayoutX2 = new BoxLayout(chunksPanel, BoxLayout.X_AXIS);
			chunksPanel.setLayout(boxlayoutX2);
			
			chunksLabel = new JLabel("N of chunks per peer [N]: ");
			chunksLabel.setPreferredSize(new Dimension(200, 30));
			
			chunksTF = new JTextField("50");
			chunksTF.setPreferredSize(new Dimension(200, 30));
			
			chunksPanel.add(chunksLabel);
			chunksPanel.add(chunksTF);
			
			// size of data chunks --- X
			bitsPanel = new JPanel();
			BoxLayout boxlayoutX3 = new BoxLayout(bitsPanel, BoxLayout.X_AXIS);
			bitsPanel.setLayout(boxlayoutX3);
			
			bitsLabel = new JLabel("Data chunk size [X]: ");
			bitsLabel.setPreferredSize(new Dimension(200, 30));
			
			bitsTF = new JTextField("60");
			bitsTF.setPreferredSize(new Dimension(200, 30));
			
			bitsPanel.add(bitsLabel);
			bitsPanel.add(bitsTF);
			

			// Threshold[Hamming distance] -- T
			thresholdPanel = new JPanel();
			BoxLayout boxlayoutX4 = new BoxLayout(thresholdPanel, BoxLayout.X_AXIS);
			thresholdPanel.setLayout(boxlayoutX4);
			
			thresholdLabel = new JLabel("Threshold [T]: ");
			thresholdLabel.setPreferredSize(new Dimension(200, 30));
			
			thresholdTF = new JTextField("28");
			thresholdTF.setPreferredSize(new Dimension(200, 30));
			
			thresholdPanel.add(thresholdLabel);
			thresholdPanel.add(thresholdTF);
			
			
			//start button
			startButton = new JButton("START");
			startButton.setPreferredSize(new Dimension(120, 40));
			startButton.setEnabled(true);
			startButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent evt) {
	            	String peersNum = peersTF.getText();
	            	String chunksNum = chunksTF.getText();
	            	String chunkSize = bitsTF.getText();
	        		String threshold = thresholdTF.getText();
	        		
	             	try {
	             		helperAgent.CreatePeerAgents(peersNum, chunksNum, chunkSize, threshold);
					} catch (ControllerException e) {
						e.printStackTrace();
					}
	            }
	        });
			
			stopButton = new JButton("STOP");
			stopButton.setPreferredSize(new Dimension(120, 40));
			stopButton.setEnabled(false);
			stopButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent evt) {
	             	helperAgent.stopSystem();
	            }
	        });
			
			buttonsPanel = new JPanel();
			BoxLayout boxlayoutX5 = new BoxLayout(thresholdPanel, BoxLayout.X_AXIS);
			thresholdPanel.setLayout(boxlayoutX5);
			
			buttonsPanel.add(startButton);
			buttonsPanel.add(stopButton);
			
			logTA = new JTextArea();
			logTA.setEditable(false);
			logTA.setPreferredSize(new Dimension(400, 40));
			logTA.setBorder(BorderFactory.createEmptyBorder(5,3,3,3));
			
			
			
			//add all elements to main panel
			mainPanel.add(peersPanel);
			mainPanel.add(chunksPanel);
			mainPanel.add(bitsPanel);
			mainPanel.add(thresholdPanel);
			mainPanel.add(buttonsPanel);
			mainPanel.add(logTA);
			
			mainPanel.setBorder(BorderFactory.createEmptyBorder(5,10,10,10));
			
			// "super" Frame sets to FlowLayout
			setLayout(new FlowLayout());  
			add(mainPanel);
			setTitle("Helper Agent"); 
		    setSize(500, 260); 
		
			
	}
	
	
	
	public void showGUI() {
		//	pack();
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int centerX = (int)screenSize.getWidth() / 2;
			int centerY = (int)screenSize.getHeight() / 2;
			setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
			super.setVisible(true);
		
		}

}

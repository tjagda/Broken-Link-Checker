package blc;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;

public class BLCgui extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField textFieldUrl;
	private final ButtonGroup buttonGroupBrowser = new ButtonGroup();
	private JTextField textFieldSaveTo;
	private JTextField textFieldLimit;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {

		// Set drivers
		System.setProperty("webdriver.ie.driver", "drivers\\IEDriverServer.exe");
		System.setProperty("webdriver.chrome.driver", "drivers\\chromedriver.exe");
		System.setProperty("webdriver.gecko.driver", "drivers\\geckodriver.exe");
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					BLCgui frame = new BLCgui();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * 
	 * @param message
	 * @param textPaneMessages
	 * @param style
	 */
	public void appendToTextPane(String message, JTextPane textPaneMessages, SimpleAttributeSet style) {
		try {
			textPaneMessages.getDocument().insertString(textPaneMessages.getDocument().getLength(),	message, style);
			textPaneMessages.setCaretPosition(textPaneMessages.getDocument().getLength());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the frame.
	 */
	public BLCgui() {
		// Set text pane fonts
		SimpleAttributeSet bold = new SimpleAttributeSet();
		StyleConstants.setBold(bold, true);
		SimpleAttributeSet error = new SimpleAttributeSet();
		StyleConstants.setForeground(error, Color.RED);
		
		// Made with built-in GUI creator
		setTitle("Co-operators Broken Link Checker ©");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 659, 516);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		// About menu button action
		JMenu mnAbout = new JMenu("About");
		mnAbout.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					java.awt.Desktop.getDesktop().edit(new File("About.docx"));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		menuBar.add(mnAbout);
		
		// Help menu button action
		JMenu mnHelp = new JMenu("Help");
		mnHelp.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JOptionPane.showMessageDialog(null, "How to use:\n1) Fill in the required fields\n2) Click \"Scan\"\n3) Read results from Excel file", "Help", JOptionPane.QUESTION_MESSAGE);
			}
		});
		menuBar.add(mnHelp);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);

		JLabel lblUrl = new JLabel("URL:");
		lblUrl.setToolTipText("URL to scan");

		textFieldUrl = new JTextField();
		textFieldUrl.setColumns(10);

		JLabel lblBrowser = new JLabel("Browser:");
		lblBrowser.setToolTipText("URL browser used for screenshots. Click on the checkbox to enable/disable.");

		JRadioButton rdbtnIe = new JRadioButton("IE");
		rdbtnIe.setToolTipText("URL browser used for screenshots. Click on the checkbox to enable/disable.");
		rdbtnIe.setEnabled(false);
		rdbtnIe.setSelected(true);
		buttonGroupBrowser.add(rdbtnIe);

		JRadioButton rdbtnFirefox = new JRadioButton("Firefox");
		rdbtnFirefox.setToolTipText("URL browser used for screenshots. Click on the checkbox to enable/disable.");
		rdbtnFirefox.setEnabled(false);
		buttonGroupBrowser.add(rdbtnFirefox);

		JRadioButton rdbtnChrome = new JRadioButton("Chrome");
		rdbtnChrome.setToolTipText("URL browser used for screenshots. Click on the checkbox to enable/disable.");
		rdbtnChrome.setEnabled(false);
		buttonGroupBrowser.add(rdbtnChrome);

		JButton btnStop = new JButton("Stop");
		btnStop.setToolTipText("Stop scan");
		btnStop.setEnabled(false);

		// Checkbox button action
		JCheckBox chckbxSaveScreenshots = new JCheckBox("Save screenshots?");
		chckbxSaveScreenshots.setToolTipText("URL browser used for screenshots. Click on this checkbox to enable/disable.");
		chckbxSaveScreenshots.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// Enables all check boxes if enabled
				if (chckbxSaveScreenshots.isSelected()) {
					rdbtnIe.setEnabled(true);
					rdbtnFirefox.setEnabled(true);
					rdbtnChrome.setEnabled(true);
				} else {
					rdbtnIe.setEnabled(false);
					rdbtnFirefox.setEnabled(false);
					rdbtnChrome.setEnabled(false);
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		JTextPane textPaneMessages = new JTextPane();
		textPaneMessages.setEditable(false);

		JSlider slider = new JSlider();
		slider.setToolTipText("The limit of the amount of unique links scanned (0 = no limit)");
		slider.setMaximum(1000);
		
		// Slider function to change the text field if slider is 0
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				try {
					textFieldLimit.setText(Integer.toString(slider.getValue()));
					
					if (textFieldLimit.getText().equals("0")) {
						textFieldLimit.setText("No Limit");
					}
				} catch (Exception e) {
				}
			}
		});
		
		JProgressBar progressBar = new JProgressBar();
		progressBar.setToolTipText("Progress Bar");
		progressBar.setStringPainted(true);
		progressBar.setString("%");
		
		textFieldLimit = new JTextField();
		textFieldLimit.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent arg0) {
				try {
					if (textFieldLimit.getText().length() > 0) {
						slider.setValue(Integer.parseInt(textFieldLimit.getText()));
					}
				} catch (Exception e) {
				}
			}
		});
		textFieldLimit.setToolTipText("The limit of the amount of unique links scanned (0 = No Limit)");
		textFieldLimit.setHorizontalAlignment(SwingConstants.CENTER);
		textFieldLimit.setText("50");
		textFieldLimit.setColumns(10);
		
		JButton btnScan = new JButton("Scan");
		btnScan.setToolTipText("Start scan");
		btnScan.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (textFieldUrl.getText().length() > 0 && textFieldSaveTo.getText().length() > 0 && textFieldLimit.getText().length() > 0) {
					char browserType = '0';

					// Determine browser type
					if (chckbxSaveScreenshots.isSelected()) {
						if (rdbtnIe.isSelected())
							browserType = 'i';
						if (rdbtnFirefox.isSelected())
							browserType = 'f';
						if (rdbtnChrome.isSelected())
							browserType = 'c';
					}

					// Run the normal threaded version of BLC
					try {
						int limit;
						
						if (textFieldLimit.getText().equals("No Limit")) {
							limit = 0;
						} else {
							limit = Integer.parseInt(textFieldLimit.getText());
						}
						
						
						MainBLCRunnable brokenLinkScan = new MainBLCRunnable(textFieldSaveTo.getText(), limit, textFieldUrl.getText().trim(), browserType, textPaneMessages, btnScan, btnStop, progressBar);
						btnScan.setEnabled(false);
						btnStop.setEnabled(true);
						brokenLinkScan.start();
					} catch (NumberFormatException e) {
						appendToTextPane("ERROR: link limit not valid\n", textPaneMessages, error);
					}
				} else {
					appendToTextPane("ERROR: all required fields are not filled\n", textPaneMessages, error);
				}
			}
		});

		// Stop button function
		btnStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Disable button to let program know to start stopping threads/processes
				btnStop.setEnabled(false);
				appendToTextPane("\nSCAN STOPPING\n\n", textPaneMessages, null);
				progressBar.setString("SCAN STOPPING");
			}
		});

		JLabel lblSaveTo = new JLabel("Save To:");
		lblSaveTo.setToolTipText("Folder location to store excel file and screenshots");

		textFieldSaveTo = new JTextField(new java.io.File(".").getAbsolutePath());
		textFieldSaveTo.setToolTipText("Folder location to store excel file and screenshots");
		textFieldSaveTo.setEditable(false);
		textFieldSaveTo.setColumns(10);

		// File change function
		JButton btnChange = new JButton("Change..");
		btnChange.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(new java.io.File("."));
				chooser.setDialogTitle("Save to..");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setAcceptAllFileFilterUsed(false);

				if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					textFieldSaveTo.setText(chooser.getSelectedFile().toString());
				}
			}
		});

		JLabel lblLimit = new JLabel("Link Limit:");
		lblLimit.setToolTipText("The limit of the amount of unique links scanned (0 = no limit)");
		
		JSeparator separator_1 = new JSeparator();

		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
						.addGroup(Alignment.LEADING, gl_contentPane.createSequentialGroup()
							.addContainerGap()
							.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 649, Short.MAX_VALUE))
						.addGroup(Alignment.LEADING, gl_contentPane.createSequentialGroup()
							.addContainerGap()
							.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_contentPane.createSequentialGroup()
									.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
										.addComponent(lblUrl)
										.addComponent(lblBrowser))
									.addGap(18)
									.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
										.addGroup(gl_contentPane.createSequentialGroup()
											.addComponent(rdbtnIe)
											.addPreferredGap(ComponentPlacement.UNRELATED)
											.addComponent(rdbtnFirefox)
											.addPreferredGap(ComponentPlacement.UNRELATED)
											.addComponent(rdbtnChrome)
											.addPreferredGap(ComponentPlacement.RELATED, 306, Short.MAX_VALUE)
											.addComponent(chckbxSaveScreenshots))
										.addComponent(textFieldUrl, GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE)))
								.addGroup(gl_contentPane.createSequentialGroup()
									.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
										.addComponent(lblSaveTo)
										.addComponent(lblLimit))
									.addGap(15)
									.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
										.addComponent(slider, GroupLayout.DEFAULT_SIZE, 487, Short.MAX_VALUE)
										.addComponent(textFieldSaveTo, GroupLayout.DEFAULT_SIZE, 487, Short.MAX_VALUE))
									.addGap(18)
									.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING, false)
										.addComponent(textFieldLimit, Alignment.LEADING, 0, 0, Short.MAX_VALUE)
										.addComponent(btnChange, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
						.addGroup(Alignment.LEADING, gl_contentPane.createSequentialGroup()
							.addGap(11)
							.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
								.addGroup(gl_contentPane.createSequentialGroup()
									.addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 466, Short.MAX_VALUE)
									.addGap(18)
									.addComponent(btnScan, GroupLayout.PREFERRED_SIZE, 79, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addComponent(btnStop, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE))
								.addComponent(separator_1, GroupLayout.DEFAULT_SIZE, 648, Short.MAX_VALUE))))
					.addContainerGap())
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(textFieldUrl, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblUrl))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(rdbtnIe)
						.addComponent(lblBrowser)
						.addComponent(rdbtnFirefox)
						.addComponent(rdbtnChrome)
						.addComponent(chckbxSaveScreenshots))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(textFieldSaveTo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnChange)
						.addComponent(lblSaveTo))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addComponent(lblLimit)
						.addComponent(textFieldLimit, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(slider, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(separator_1, GroupLayout.PREFERRED_SIZE, 2, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING, false)
						.addComponent(progressBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(btnStop, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(btnScan, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE)
					.addContainerGap())
		);

		scrollPane.setViewportView(textPaneMessages);
		contentPane.setLayout(gl_contentPane);
	}
}

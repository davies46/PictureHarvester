package com.re4ct.fileflatten;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFileChooser;

import org.apache.log4j.Logger;

public class ChooserBtn extends JButton {
	final static Logger log = Logger.getLogger(ChooserBtn.class);

	public ChooserBtn(String caption, String defualtPath, ChooserData data) {
		super(caption);
		addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser folderChooser = new JFileChooser();
				folderChooser.setCurrentDirectory(new java.io.File(defualtPath));
				folderChooser.setDialogTitle("Dest Folder");
				folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				folderChooser.setAcceptAllFileFilterUsed(false);

				if (folderChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {

					log.info("getCurrentDirectory(): " + folderChooser.getCurrentDirectory());
					String path = folderChooser.getSelectedFile().toString();
					log.info("getSelectedFile() : " + path);
					data.setPath(path);
				} else {
					log.info("No Selection ");
				}
			}
		});
	}
}

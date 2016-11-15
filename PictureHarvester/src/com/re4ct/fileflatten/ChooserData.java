package com.re4ct.fileflatten;

import javax.swing.JLabel;

public class ChooserData {

	private String	path;
	private JLabel	lbl;

	public ChooserData(JLabel folderLbl) {
		lbl = folderLbl;
	}

	public void setPath(String path) {
		this.path = path;
		lbl.setEnabled(true);
		lbl.setText(path);

	}

	public String getPath() {
		return path;
	}

}

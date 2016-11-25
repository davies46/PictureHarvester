package com.re4ct.fileflatten;

import java.awt.image.BufferedImage;

public interface VisualCallbacks {

	// void visualImageCompare(String absolutePath, String dstFileName);

	void setIterLbl(String string);

	void visualImageCompare(BufferedImage i1, BufferedImage i2);

	void setMsg(String string);

}

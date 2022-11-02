package com.udacity.catpoint.security.service;

import java.awt.*;

/**
 * Simple "service" for providing style information.
 */
public class StyleService {
    final private static Font HeadingFont = new Font("Sans Serif", Font.BOLD, 24);

    static public Font HEADING_FONT() { return HeadingFont;}

}

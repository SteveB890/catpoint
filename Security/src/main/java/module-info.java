module Security {
	requires miglayout;
	requires java.desktop;
	requires com.google.gson;
	requires com.google.common;
	requires java.prefs;
	requires Image;

	// allow for reflection used in gson module
	opens com.udacity.catpoint.security.data to com.google.gson;
}
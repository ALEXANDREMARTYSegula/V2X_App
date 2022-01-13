package com.segula.v2x.utils;

public class GlobalConstants {
    public static final String SHARED_PREFS= "shared_prefs";

    public static String lastLocationLatitude = "LAST_LOCATION_LATITUDE";
    public static String lastLocationLongitude = "LAST_LOCATION_LONGITUDE";

    private static final String SEGMENT_LANGUAGE_ENGLISH = "EN";
    private static final String SEGMENT_LANGUAGE_FRENCH = "FR";

    //public static ArrayList<stellantisIDCar>

    public enum Language {
        ENGLISH, FRENCH;

        static {
            ENGLISH.languageString = GlobalConstants.SEGMENT_LANGUAGE_ENGLISH;
            FRENCH.languageString = GlobalConstants.SEGMENT_LANGUAGE_FRENCH;
        }

        static {
            ENGLISH.languageSymbol = "EN";
            FRENCH.languageSymbol = "FR";
        }

        private String languageString;
        private String languageSymbol;

        public  String getString() {return languageString;}
        public String getSymbol() {return languageSymbol;}
    }
}

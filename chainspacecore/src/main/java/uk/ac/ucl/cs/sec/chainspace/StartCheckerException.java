package uk.ac.ucl.cs.sec.chainspace;

class StartCheckerException extends Exception {

    StartCheckerException(String message) {

        super("[ERROR_STARTCHECKER] " + message);
    }
}

public enum MsgType {
    LOGIN1,
    LOGIN2,
    ACCEPT_PASSWORD,
    NEW_PLAYER,
    REJECT_USERNAME,
    REJECT_PASSWORD,
    GAME_START,
    MOVE,
    MOVE_FEEDBACK,
    MOVE_RESULT,
    GAME_OVER,
    QUIT, GUI,
    CLIENT_CHAT,
    PLAY_AGAIN,
    DRAW
}

/* LOGIN1
* 1) Client -> Server
*       -- client sends username they want to login with (gets back one of these three: 1.LOGIN2 2.NEW_PLAYER 3.REJECT_USERNAME)
* */

/* LOGIN2
 * 1) Server -> Client
 *       a) sends LOGIN2 with empty password field --> means username got accepted and user should enter their password
 *       b) sends back LOGIN2 that user sent       --> means password got accepted and user is waiting to be paired with a player
 * 2) Client -> Server
 *       -- client sends password
 * */

/* REJECT_USERNAME
*  1) Server -> Client
*       -- sends if username exists BUT there is already an active client thread logged in with that username
* */

/* REJECT_PASSWORD
 *  1) Server -> Client
 *       -- sends if password doesn't match the password matching the username in HashMap<String, String> userPasswords
 * */
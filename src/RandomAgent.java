import java.util.Random;
import java.util.List;

public class RandomAgent implements Agent {
    private Random random = new Random();
    private String role;
    private boolean myTurn;
    private QueenBattleState state;

    public void init(String role, int width, int height, int playclock, int[][] white_positions, int[][] black_positions) {
        this.role = role;
        this.myTurn = role.equals("white"); 
        this.state = new QueenBattleState(width, height, white_positions, black_positions);
    }

    public String nextAction(int[] lastMove) {
        if (lastMove != null) {
            String oppRole = role.equals("white") ? "black" : "white";
            state.applyMove(lastMove[0], lastMove[1], lastMove[2], lastMove[3], oppRole);
        }

        if (myTurn) {
            List<int[]> legalMoves = state.getLegalMoves(this.role);
            
            if (legalMoves.isEmpty()) return "noop"; 

            int[] move = legalMoves.get(random.nextInt(legalMoves.size()));
            
            state.applyMove(move[0], move[1], move[2], move[3], this.role);
            
            myTurn = false;
            return "(play " + move[0] + " " + move[1] + " " + move[2] + " " + move[3] + ")";
        } else {
            myTurn = true;
            return "noop";
        }
    }

    @Override
    public void cleanup() {
        state = null;
    }
}
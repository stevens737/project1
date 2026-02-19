import java.util.List;

public class AlphaBetaAgent implements Agent {
    private String role;
    private int playclock;
    private boolean myTurn;
    private QueenBattleState state;
    private long startTime;
    private long timeLimitMillis;

    private static class TimeoutException extends RuntimeException {}

    public void init(String role, int width, int height, int playclock, int[][] white_positions, int[][] black_positions) {
        this.role = role;
        this.playclock = playclock;
        this.myTurn = role.equals("white");
        this.state = new QueenBattleState(width, height, white_positions, black_positions);
    }

    public String nextAction(int[] lastMove) {
        if (lastMove != null) {
            String oppRole = role.equals("white") ? "black" : "white";
            state.applyMove(lastMove[0], lastMove[1], lastMove[2], lastMove[3], oppRole);
        }

        myTurn = !myTurn;
        if (myTurn) {
            startTime = System.currentTimeMillis();
            timeLimitMillis = (playclock * 1000) - 300; // Give a small buffer on the runtime

            int[] bestMoveFound = null;
            int depth = 1;

            try {
                while (true) {
                    bestMoveFound = startNegamax(state, depth);
                    depth++;
                }
            } catch (TimeoutException e) {}

            if (bestMoveFound != null) {
                state.applyMove(bestMoveFound[0], bestMoveFound[1], bestMoveFound[2], bestMoveFound[3], this.role);
                return "(play " + bestMoveFound[0] + " " + bestMoveFound[1] + " " + bestMoveFound[2] + " " + bestMoveFound[3] + ")";
            }
        }
        
        return "noop";
    }
    

    private int[] startNegamax(QueenBattleState s, int maxDepth) {
        List<int[]> moves = s.getLegalMoves(this.role);
        int[] bestMove = moves.get(0); // Default to first legal move
        int alpha = Integer.MIN_VALUE + 1;
        int beta = Integer.MAX_VALUE - 1;
        int bestValue = Integer.MIN_VALUE + 1;

        for (int[] move : moves) {
            checkTime();
            QueenBattleState nextState = s.clone();
            nextState.applyMove(move[0], move[1], move[2], move[3], this.role);

            int val = -alphaBetaNegamax(nextState, maxDepth - 1, -beta, -alpha, false);
            if (val > bestValue) {
                bestValue = val;
                bestMove = move;
            }
            alpha = Math.max(alpha, bestValue);
        }
        return bestMove;
    }

    private int alphaBetaNegamax(QueenBattleState s, int depth, int alpha, int beta, boolean isMyTurn){
        checkTime();
        int terminalScore = s.evaluate(this.role);
        // Return score relative to the player whose turn it is
        if (depth <= 0 || Math.abs(terminalScore) == 100 || terminalScore == 0) {
            return isMyTurn ? terminalScore : -terminalScore;
        }

        int bestValue = Integer.MIN_VALUE + 1;
        String currentRole = isMyTurn ? this.role : (this.role.equals("white") ? "black" : "white");
        
        for (int[] move : s.getLegalMoves(currentRole)) {
            QueenBattleState next = s.clone();
            next.applyMove(move[0], move[1], move[2], move[3], currentRole);
            
            int value = -alphaBetaNegamax(next, depth - 1, -beta, -alpha, !isMyTurn);
            
            bestValue = Math.max(value, bestValue);
            if (bestValue > alpha) {
                alpha = bestValue;
                if (alpha >= beta) break; // Beta cutoff [image_7cf49e.png]
            }
        }
        return bestValue;
    }

    private void checkTime() {
        if (System.currentTimeMillis() - startTime >= timeLimitMillis) 
            throw new TimeoutException();
    }

    public void cleanup() { 
        state = null; 
    }
}
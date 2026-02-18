import java.util.List;

public class QueenBattleTest {
    public static void main(String[] args) {
        try {
            testMovementAndBurning();
            testDiagonalAndEdgeBoundaries();
            testObstacleAndBurnedPath();
            testTerminalWinLoss();
            testDrawByTileLimit();
            System.out.println("\nALL TESTS PASSED!");
        } catch (AssertionError e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Task 2 verification: Pieces move and leave burned squares [cite: 15]
    public static void testMovementAndBurning() {
        int[][] whitePos = {{1, 1}};
        int[][] blackPos = {{4, 4}};
        QueenBattleState state = new QueenBattleState(4, 4, whitePos, blackPos);

        state.applyMove(1, 1, 1, 3, "white");

        if (state.board[1][1] != -1) throw new AssertionError("Square 1,1 should be burned (-1)");
        if (state.board[1][3] != 1) throw new AssertionError("Square 1,3 should contain white queen (1)");
        System.out.println("testMovementAndBurning: Passed");
    }

    // Task 2 verification: Queens move horizontally, vertically, and diagonally [cite: 14]
    public static void testDiagonalAndEdgeBoundaries() {
        // Board is 4x4, so positions must be between 1 and 4
        int[][] whitePos = {{2, 2}};
        int[][] blackPos = {{4, 1}}; // Changed from 10,10 to fit a 4x4 board
        QueenBattleState state = new QueenBattleState(4, 4, whitePos, blackPos);

        List<int[]> moves = state.getLegalMoves("white");
        boolean foundDiagonal = false;
        for (int[] m : moves) {
            // Check move to the far corner (4,4) from (2,2)
            if (m[2] == 4 && m[3] == 4) foundDiagonal = true;
            
            // Validate that no move coordinates exceed the width/height
            if (m[2] < 1 || m[2] > 4 || m[3] < 1 || m[3] > 4) 
                throw new AssertionError("Move out of board bounds: " + m[2] + "," + m[3]);
        }
        if (!foundDiagonal) throw new AssertionError("Failed to find valid diagonal move to 4,4");
        System.out.println("testDiagonalAndEdgeBoundaries: Passed");
    }

    // Task 2 verification: Pieces cannot pass over other pieces or blocked squares 
    public static void testObstacleAndBurnedPath() {
        int[][] whitePos = {{1, 1}};
        int[][] blackPos = {{4, 4}};
        QueenBattleState state = new QueenBattleState(4, 4, whitePos, blackPos);

        // Burn square 1,2 to block vertical path
        state.board[1][2] = -1; 
        
        List<int[]> moves = state.getLegalMoves("white");
        for (int[] m : moves) {
            if (m[2] == 1 && m[3] == 3) throw new AssertionError("White jumped over a burned square at 1,2!");
        }
        System.out.println("testObstacleAndBurnedPath: Passed");
    }

    // Task 3 verification: Correct win/loss scores [cite: 46, 48]
    public static void testTerminalWinLoss() {
        int[][] whitePos = {{1, 1}};
        int[][] blackPos = {{4, 4}};
        QueenBattleState state = new QueenBattleState(4, 4, whitePos, blackPos);

        // Surround white completely
        state.board[1][2] = -1; state.board[2][1] = -1; state.board[2][2] = -1;

        if (state.evaluate("white") != -100) throw new AssertionError("White should have score -100 for loss");
        System.out.println("testTerminalWinLoss: Passed");
    }

    // Task 3 verification: Game ends in draw if only W empty squares left [cite: 19, 21, 47]
    public static void testDrawByTileLimit() {
        int W = 4;
        int[][] whitePos = {{1, 1}};
        int[][] blackPos = {{4, 4}};
        QueenBattleState state = new QueenBattleState(W, 4, whitePos, blackPos);

        // Burn squares until only W (4) empty squares remain
        // Total squares = 16. Pieces use 2. Need 10 burned squares to leave 4 empty.
        int burnedCount = 0;
        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 4; j++) {
                if (state.board[i][j] == 0 && burnedCount < 10) {
                    state.board[i][j] = -1;
                    burnedCount++;
                }
            }
        }

        if (state.evaluate("white") != 0) throw new AssertionError("Should be a draw (0) because only W empty squares remain");
        System.out.println("testDrawByTileLimit: Passed");
    }
}
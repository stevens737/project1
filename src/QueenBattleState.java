import java.util.ArrayList;
import java.util.List;

public class QueenBattleState {
    public int width, height;
    public int[][] board; // 0: empty, 1: white, 2: black, -1: burned
    public List<int[]> whiteQueens;
    public List<int[]> blackQueens;

    public QueenBattleState(int w, int h, int[][] whitePos, int[][] blackPos) {
        this.width = w;
        this.height = h;
        this.board = new int[w + 1][h + 1];
        this.whiteQueens = new ArrayList<>();
        this.blackQueens = new ArrayList<>();

        for (int[] p : whitePos) {
            board[p[0]][p[1]] = 1;
            whiteQueens.add(new int[]{p[0], p[1]});
        }
        for (int[] p : blackPos) {
            board[p[0]][p[1]] = 2;
            blackQueens.add(new int[]{p[0], p[1]});
        }
    }

    public void applyMove(int x1, int y1, int x2, int y2, String role) {
        int pieceType = role.equals("white") ? 1 : 2;
        board[x1][y1] = -1; // Original square is burned 
        board[x2][y2] = pieceType;

        List<int[]> queens = role.equals("white") ? whiteQueens : blackQueens;
        for (int[] q : queens) {
            if (q[0] == x1 && q[1] == y1) {
                q[0] = x2;
                q[1] = y2;
                break;
            }
        }
    }

    public List<int[]> getLegalMoves(String role) {
        List<int[]> moves = new ArrayList<>();
        List<int[]> queens = role.equals("white") ? whiteQueens : blackQueens;
        int[][] directions = {{0,1}, {0,-1}, {1,0}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};

        for (int[] q : queens) {
            for (int[] d : directions) {
                for (int dist = 1; ; dist++) {
                    int nx = q[0] + d[0] * dist;
                    int ny = q[1] + d[1] * dist;
                    if (nx >= 1 && nx <= width && ny >= 1 && ny <= height && board[nx][ny] == 0) {
                        moves.add(new int[]{q[0], q[1], nx, ny});
                    } else {
                        break; // Blocked by piece, burned square, or edge 
                    }
                }
            }
        }
        return moves;
    }

    public int evaluate(String role) {
        // 1. Calculate empty squares first to check the global draw condition 
        int emptySquares = 0;
        for (int i = 1; i <= width; i++) {
            for (int j = 1; j <= height; j++) {
                if (board[i][j] == 0) emptySquares++;
            }
        }

        // 2. Draw condition: W or fewer empty squares left 
        if (emptySquares <= width) {
            return 0; // Draw [cite: 47]
        }

        // 3. Check mobility for terminal states [cite: 18, 20]
        List<int[]> whiteMoves = getLegalMoves("white");
        List<int[]> blackMoves = getLegalMoves("black");
        boolean whiteCanMove = !whiteMoves.isEmpty();
        boolean blackCanMove = !blackMoves.isEmpty();

        // If BOTH are unable to move, it's a draw [cite: 21]
        if (!whiteCanMove && !blackCanMove) {
            return 0; // Draw [cite: 47]
        }
        // If only white is stuck, white loses [cite: 20]
        if (!whiteCanMove) {
            return -100; // White lost [cite: 48]
        }
        // If only black is stuck, white wins [cite: 20]
        if (!blackCanMove) {
            return 100; // White won [cite: 46]
        }

        // 4. Heuristic for non-terminal states [cite: 49]
        return countMoveableQueens("white") - countMoveableQueens("black");
    }

    private int countMoveableQueens(String role) {
        int count = 0;
        List<int[]> queens = role.equals("white") ? whiteQueens : blackQueens;
        int[][] directions = {{0,1}, {0,-1}, {1,0}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};

        for (int[] q : queens) {
            for (int[] d : directions) {
                int nx = q[0] + d[0];
                int ny = q[1] + d[1];
                // If there is at least one legal square to move to, the queen is moveable
                if (nx >= 1 && nx <= width && ny >= 1 && ny <= height && board[nx][ny] == 0) {
                    count++;
                    break; 
                }
            }
        }
        return count;
    }

    public QueenBattleState clone() {
        int[][] newWhitePos = new int[whiteQueens.size()][2];
        for (int i = 0; i < whiteQueens.size(); i++) newWhitePos[i] = whiteQueens.get(i).clone();
        
        int[][] newBlackPos = new int[blackQueens.size()][2];
        for (int i = 0; i < blackQueens.size(); i++) newBlackPos[i] = blackQueens.get(i).clone();
        
        QueenBattleState copy = new QueenBattleState(width, height, newWhitePos, newBlackPos);
        for (int i = 0; i <= width; i++) copy.board[i] = board[i].clone();
        return copy;
    }
}
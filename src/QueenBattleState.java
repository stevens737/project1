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
}
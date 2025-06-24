public interface ISolver {
    MinesweeperGame.Move findNextMove(char[][] currentBoard, int totalBombs);
}
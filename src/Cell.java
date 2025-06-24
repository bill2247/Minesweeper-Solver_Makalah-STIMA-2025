public class Cell {
    public boolean isBomb;
    public boolean isRevealed;
    public boolean isFlagged;
    public int adjacentBombs;

    public Cell() {
        this.isBomb = false;
        this.isRevealed = false;
        this.isFlagged = false;
        this.adjacentBombs = 0;
    }

    @Override
    public String toString() {
        if (isFlagged) return "F";
        if (!isRevealed) return "C";
        if (isBomb) return "B";
        if (adjacentBombs == 0) return "_";
        return String.valueOf(adjacentBombs);
    }
}
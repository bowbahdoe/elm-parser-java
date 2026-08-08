void randomSpaces(StringBuilder sb) {
    sb.append(" ".repeat((int) (8 * Math.random())));
    sb.append("\n".repeat((int) (8 * Math.random())));
    sb.append(" ".repeat((int) (8 * Math.random())));
    sb.append("\n".repeat((int) (8 * Math.random())));
}
void main() throws Exception {
    var sb = new StringBuilder();
    sb.append("[");
    var t = 10000000;
    for (int i = 0; i < t; i++) {
        randomSpaces(sb);
        sb.append("(");
        sb.append((int) (100000 * Math.random()));
        randomSpaces(sb);
        sb.append(",");
        randomSpaces(sb);
        sb.append((int) (100000 * Math.random()));
        sb.append(")");
        randomSpaces(sb);
        if (!(i == t - 1)) {
            sb.append(";");
        }
    }
    sb.append("]");

    Files.writeString(Path.of("points.txt"), sb.toString());
}
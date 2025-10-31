package index.learned_node_info;

public class Model {
    double slop;
    double inter;

    public Model() {
    }
    public Model(double slop, double inter) {
        this.slop = slop;
        this.inter = inter;
    }

    public int find(long tar) {
        return  (int) (slop * (tar - inter));
    }
}

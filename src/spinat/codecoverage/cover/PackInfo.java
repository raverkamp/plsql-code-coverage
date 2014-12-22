package spinat.codecoverage.cover;

public class PackInfo {

    public int id;
    public final String owner;
    public final String name;
    public boolean isCovered;
    public java.util.Date start;
    public java.util.Date end;
    public boolean isValid;

    public PackInfo(int id, String owner, String name, boolean isCovered, java.util.Date start, java.util.Date end, boolean isValid) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.isCovered = isCovered;
        this.start = start;
        this.end = end;
        this.isValid = isValid;
    }

    @Override
    public String toString() {
        return "PackInfo{ id=" + this.id
                + ", owner=" + this.owner
                + ", name=" + this.name
                + ", isCovered=" + this.isCovered
                + ", start=" + this.start
                + ", end=" + this.end
                + ", isValid=" + this.isValid + "}";
    }

}

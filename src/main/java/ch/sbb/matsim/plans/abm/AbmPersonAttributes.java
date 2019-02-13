package ch.sbb.matsim.plans.abm;

public class AbmPersonAttributes {

    private int ageCat;
    private int emplPctCat;
    private int eduType;


    public AbmPersonAttributes(int ageCat, int emplPctCat, int eduType) {

        this.ageCat = ageCat;
        this.emplPctCat = emplPctCat;
        this.eduType = eduType;
    }

    public int getAgeCat() {
        return ageCat;
    }

    public int getEmplPctCat() {
        return emplPctCat;
    }

    public int getEduType() {
        return eduType;
    }

}

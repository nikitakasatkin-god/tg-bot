package org.example;

class CRIBData {
    private int weight;
    private int gestation;
    private String congenital;
    private double baseExcess;
    private double minFio2;
    private double maxFio2;

    // Геттеры и сеттеры
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public int getGestation() { return gestation; }
    public void setGestation(int gestation) { this.gestation = gestation; }

    public String getCongenital() { return congenital; }
    public void setCongenital(String congenital) { this.congenital = congenital; }

    public double getBaseExcess() { return baseExcess; }
    public void setBaseExcess(double baseExcess) { this.baseExcess = baseExcess; }

    public double getMinFio2() { return minFio2; }
    public void setMinFio2(double minFio2) { this.minFio2 = minFio2; }

    public double getMaxFio2() { return maxFio2; }
    public void setMaxFio2(double maxFio2) { this.maxFio2 = maxFio2; }
}
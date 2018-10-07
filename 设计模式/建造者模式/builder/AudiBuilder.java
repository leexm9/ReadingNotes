package builder;

import java.util.ArrayList;

public class AudiBuilder extends CarBuilder {
    
    private Audi audi = new Audi();
    @Override
    public void setSequence(ArrayList<String> sequence) {
        this.audi.setSequence(sequence);
    }

    @Override
    public CarModel getCarModel() {
        return this.audi;
    }

}

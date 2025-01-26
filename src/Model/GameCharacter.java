package Model;

import Technical.PowerEnum;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public abstract class GameCharacter {

    protected final String name;

    protected Image image;

    protected String description;

    protected boolean nocturnal = false;

    protected boolean infected = false;

    protected PowerEnum power = PowerEnum.Constant;

    public GameCharacter(String name, String description, boolean isNocturnal, PowerEnum powerType, String imagePath) {
        this.name = name;
        this.description = description;
        nocturnal = isNocturnal;
        power = powerType;

        //Handling image
        File file = new File(imagePath);

        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public Image getImage() {
        return image;
    }

    public String getDescription() {
        return description;
    }

    public boolean isNocturnal() {
        return nocturnal;
    }

    public boolean isInfected() {
        return infected;
    }

    public void changeNocturnal() {
        nocturnal = !nocturnal;
    }

    public PowerEnum getPower() {
        return power;
    }

    public void setPower(PowerEnum power) {
        this.power = power;
    }

    public void SetInfected(boolean infected) {
        this.infected = infected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameCharacter gameCharacter = (GameCharacter) o;
        return nocturnal == gameCharacter.nocturnal &&
                name.equals(gameCharacter.name) &&
                image.equals(gameCharacter.image) &&
                description.equals(gameCharacter.description) &&
                power == gameCharacter.power;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, image, description, nocturnal, power);
    }
}

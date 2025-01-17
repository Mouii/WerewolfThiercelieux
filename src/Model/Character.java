package Model;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public abstract class Character {

    protected final String name;

    protected Image image;

    protected String description;

    protected boolean nocturnal = false;

    protected PowerEnum power = PowerEnum.Constant;

    public Character(String name, String description, boolean isNocturnal, PowerEnum powerType, String imagePath) {
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

    public void changeNocturnal() {
        nocturnal = !nocturnal;
    }

    public PowerEnum getPower() {
        return power;
    }

    public void setPower(PowerEnum power) {
        power = power;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Character character = (Character) o;
        return nocturnal == character.nocturnal &&
                name.equals(character.name) &&
                image.equals(character.image) &&
                description.equals(character.description) &&
                power == character.power;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, image, description, nocturnal, power);
    }
}

package com.brackeen.javagamebook.tilegame.sprites;

import com.brackeen.javagamebook.graphics.Animation;

/**
    A Grub is a Creature that moves slowly on the ground.
*/
public class Grub extends Creature {

    private boolean on_screen; // Are we on the screen?

    public boolean allow_shooting; // Can we shoot?

    public float shooting_time; // time since last shot

  	public float wait_time; // Time we have waited since facing player on screen

    public boolean first_shot; // Have we taken our first shot?

    public float relative_position; // player's position upon on_screen entry! Compare to "2 Units of Movement"

    public Grub(Animation left, Animation right,
        Animation deadLeft, Animation deadRight)
    {
        super(left, right, deadLeft, deadRight);
        on_screen = false;
        wait_time = 0;
    }


    public boolean isOnScreen(){return on_screen;}

    public void setOnScreen(boolean value){
    	on_screen = value;
    }

    public float getMaxSpeed() {
        return 0.03f;
    }

}

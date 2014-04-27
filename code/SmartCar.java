
import java.util.*;
import java.awt.geom.*;
import java.awt.*;


public class SmartCar {
    public final double CAR_MIN_SPACING = 40;
    public final double THETA_THRESHOLD = .1;
    public final double ROTATION_RATE = 25;
    public final double TURNING_ANGLE_DEGREES = 60;
    public final double ROAD_Y_THRESHOLD = 1;
    public final double STRAIGHTEN_ADJUSTMENT = 0;
    public final double DECEL_RATE = .66;

    // The two controls: either (vel,phi) or (acc,phi)
    double acc;       // Acceleration.
    double vel;       // Velocity.
    double phi;       // Steering angle.
    double x;       //X coordinate
    double y;       //Y coordinate
    double theta;
    int lane;
    int newlane;
    double targetVel; //Velocity car wants to maintain
    double distMoved;
    boolean changingLanes;
    boolean isStraightening;
    Road road;
    boolean isSpeeder;

    double width = 34;
    double height = 16;


    boolean DEBUG;

    ArrayList<Rectangle2D.Double> obstacles;
    //    SensorPack sensors;

    // Is the first control an accelerator?
    boolean isAccelModel = false;

    /**
     * [init description]
     * @param initX     [description]
     * @param initY     [description]
     * @param initTheta [description]
     * @param endX      [description]
     * @param endY      [description]
     * @param endTheta  [description]
     * @param obstacles [description]
     * @param sensors   [description]
     */
    public SmartCar (int lane, double initTheta, double startSpeed, Road thisRoad, boolean debug, boolean isSpeeder) {
        //        this.obstacles = obstacles;
        //        this.sensors = sensors;
        //        this.lane = lane;
        this.vel = this.targetVel = startSpeed;
        this.phi = 0;
        this.x = 0;
        this.theta = initTheta;
        this.lane = lane;
        this.road = thisRoad;
        this.y = road.getLaneCenter(lane);

        this.isStraightening = false;
        this.changingLanes = false;
        this.isSpeeder = isSpeeder;

        this.DEBUG = debug;
        distMoved = 0.0;
    }

    /**
     * [getControl description]
     * @param  i [description]
     * @return   [description]
     */
    public double getControl (int i) {
        if (i == 1) {
            if (isAccelModel) {
                return acc;
            } else {
                return vel;
            }
        } else if (i == 2) {
            return phi;
        }
        return 0;
    }


    public void move () {
        if (changingLanes) checkChangingLanes();
        else if (isStraightening) {
            //If straightened out, stop rotating
            if (straightenedOut()) {
                phi = 0;
                isStraightening = false;
            }
        }
        //If not changing lanes or straightening, do logic
        else {
            phi = 0;

            if (isSpeeder) {
                //Speeder behaves differently than law abiding cars

                //If about to hit car, first try to change lanes to get by
                if (tooCloseToCar()) {
                    if (!changeLanes(true)) {
                        if (!changeLanes(false)) {
                            //If unable to change lanes (both left and right return false), slow down
                            if (DEBUG) System.out.println("Speeder slowing down");

                            vel = vel * DECEL_RATE;
                        }
                    }
                }
                //If not about to hit car, return to targetVelocity
                else vel = targetVel;
            }
            //If not speed, first check if about to hit car
            else {
                //Check if about to hit car. If so, slow down
                if (tooCloseToCar()) {
                    if (DEBUG) System.out.println("Law abider slowing down");
                    vel = vel * DECEL_RATE;
                }
                //CHECK HERE FOR A SPEEDER
                else vel = targetVel;
            }
        }



        //
        // logic:
        //
        // am i about to hit a car
        // should i change lanes?
        // tooCloseToCar()
        //
        //if any car is speeding and not i'm changing lanes
        //is speeder in my lane
        //are/is other car(s) in the other lane(s)?
        //
        //do i need to change lanes to stop speeder?
        //
        //}

    }

    /**
     * changes lanes for car
     * returns false if not possible (or fails)
     * updates lane value
     * @return [description]
     */
    public boolean tooCloseToCar() {
        ArrayList<SmartCar> cars = road.getCars();
        for (SmartCar c : cars) {
            //Car ignores itself
            if (!c.equals(this)) {
                //Check if in the same lane
                if (this.lane == c.lane) {
                    //Check if car is in front and too close
                    if ((c.x > this.x) && ((c.x - this.x) < CAR_MIN_SPACING)) {
                        if (DEBUG) System.out.println("Car detected in front");
                        return true;
                    }
                }
            }
        }
        return false;

        //use distance function to compare x values of cars
        //change lanes if necessary
    }

    public void draw (Graphics2D g2, Dimension D) {
        // If you want do draw something on the screen (e.g., a path)
        // this is where you do it. Remember to convert to Java coordinates:
        //    yJava = D.height - y;
        //    paramGraphics2D.setColor(Color.cyan);
    }

    /**
     * changes lanes for car
     * returns false if not possible (or fails)
     * updates lane value
     * @return [description]
     */
    public boolean changeLanes(boolean left) {
        if (!changingLanes && !isStraightening) {
            if (left) {
                if (lane == 1) {
                    if (DEBUG) System.out.println("Cannot change lanes left");
                    return false;
                } else {
                    newlane = lane - 1;
                    this.phi = ROTATION_RATE;
                }
            } else {
                if (lane == road.numLanes) {
                    if (DEBUG) System.out.println("Cannot change lanes right");
                    return false;
                } else {
                    newlane = lane + 1;
                    this.phi = -ROTATION_RATE;
                }
            }

            changingLanes = true;
            return true;
        }
        if (DEBUG) System.out.println("Unable to change lanes: Car is currently changing or straightening");
        return false;
    }

    public void checkChangingLanes() {
        if (this.changingLanes) {
            double angleDist = Math.abs(this.theta - TURNING_ANGLE_DEGREES * Math.PI / 180);
            double secondDist = Math.abs((Math.PI * 2 - this.theta) + TURNING_ANGLE_DEGREES * Math.PI / 180);
            if (secondDist < angleDist) angleDist = secondDist;


            //If at turning angle, set phi to zero
            if (angleDist > THETA_THRESHOLD) {
                phi = 0;
            }

            //If at y, rotate back to theta = 0;
            if (Math.abs(this.y - road.getLaneCenter(newlane) - STRAIGHTEN_ADJUSTMENT) < ROAD_Y_THRESHOLD) {
                isStraightening = true;
                changingLanes = false;
                this.lane = newlane;

                //Find shortest rotation back to zero (will be the reverse direction it rotated to turn)
                if (theta > Math.PI) phi = ROTATION_RATE;
                else phi = -ROTATION_RATE;
            }


            //set phi/vel
            //if y val is target lane y
            //straighten out
            //if straightened out
            //changing lanes = false


        }
        return;
    }

    public boolean straightenedOut() {
        if ((this.theta < THETA_THRESHOLD) || (2 * Math.PI - this.theta < THETA_THRESHOLD)) return true;
        else return false;
    }

    /**
     * adjust to speed to parameter
     * @param v velocity to change to
     */
    public void adjustSpeed(double v) {
        if (DEBUG) System.out.println("Velocity changing to: " + v * 10 + " mph");
        this.vel = v;
    }



}

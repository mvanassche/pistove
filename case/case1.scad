
width = 135;
height = 180;
depth = 50;

wallThickness = 2;



difference() {
    cube([height, width, depth - wallThickness]);
        // general box
    translate([-1, wallThickness, wallThickness])
        cube([height - wallThickness + 1, width - (2 * wallThickness) , depth + 1]);
        // bottom hole
    translate([30, 20, -1])
        cube([45, 80, wallThickness * 2]);
        // cable hole
    translate([150, 109 - 5, 0]) rotate([90, 0, 90])
        cylinder(h = height, r = 5);
    translate([40, 100 - 5, -1])
        cube([120, 5, 15]);

        // cover ridge
    translate([2, 2, height - 2])
        cube([height - 4, width - 4, 5]);
        // side buttons hole
    translate([16, -1, depth - 16])
        cube([54, 10, 8]);
    
        // ventilation bottom
    for (i = [0 : (width / 7)]){
        translate([height - wallThickness - 1, 10 + (i * 6), 10])
            cube([5, 3, depth - 20]);
    }    

    // hole for auto button
    translate([height - 15, 109 - 5, 33]) rotate([0, 90, 90])
        cylinder(h = height, r = 8);

    // hole for rotary button
    translate([height - 15, 109 - 5, 13]) rotate([0, 90, 90])
        cylinder(h = height, r = 4);
    
}

    // support max's
/*translate([height - 80 - 40, width - 10 - wallThickness, 22])
    cube([80, 11, wallThickness]);*/
translate([height - 80 - 40, width - 4, 0])
    cylinder(22, r = 2.5);



// cover
color("blue")
//translate([0, width + 20, 0])
rotate([180, 0, 0]) translate([0, 10, -depth])
difference() {
    cube([height + wallThickness, width, depth]);
    translate([wallThickness, -1, -1])
        cube([height + 10, width + 2, depth - wallThickness + 1]);
    
    // screen hole
    translate([14, 8, depth - wallThickness - 1])
        cube([59, 98, wallThickness * 2]);
    
        // ventilation top
    for (i = [0 : (width / 7)]){
        translate([-1, 10 + (i * 6), 10])
            cube([5, 3, depth - 20]);
    }

}

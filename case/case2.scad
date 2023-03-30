
width = 112;
height = 200;
depth = 50;

wallThickness = 2;
maxWidth = 21; // MAX81355



difference() {
    cube([height, width, depth - wallThickness]);
        // general box
    translate([-1, wallThickness, wallThickness])
        cube([height - wallThickness + 1, width - (2 * wallThickness) , depth + 1]);
    
        // bottom hole
    translate([20, 20, -1])
        cube([50, 70, wallThickness * 2]);
    
    translate([height - 50, 75, -1])
        cube([40, 16, wallThickness * 2]);
    
        // cable hole
    translate([height - 25, 20 - 5, 0]) rotate([90, 0, 0])
        cylinder(h = height, r = 5);
    translate([height - 55, 10, -1])
        cube([46, 21, 15]);

        // cover ridge
    translate([2, 2, height - 2])
        cube([height - 4, width - 4, 5]);
    
        // side buttons hole
    translate([16, -1, depth - 16])
        cube([54, 10, 8]);
    
        // ventilation bottom
    margin = 10;
    for (i = [0 : ((width - 2*margin) / 6)]){
        translate([height - wallThickness - 1, margin + (i * 6), 10])
            cube([5, 3, depth - 20]);
    }    

    // hole for auto button
    translate([height - 15, 109 - 5, 33]) rotate([0, 90, 90])
        cylinder(h = height, r = 8);

    // hole for rotary button
    translate([height - 15, 109 - 5, 13]) rotate([0, 90, 90])
        cylinder(h = height, r = 4);

    // support screen
    translate([14, (width - 99) / 2, -1]) {
        translate([0, 0, 0])
            cylinder(10, r = 1.5);
        
        translate([0, 99, 0])
            cylinder(10, r = 1.5);

        translate([60, 99, 0])
            cylinder(10, r = 1.5);

        translate([60, 0, 0])
            cylinder(10, r = 1.5);
    }
    
    // holes for screws wall
    translate([40, 10, -1])
        cylinder(10, r = 4);
    translate([36, 10, -1])
        cylinder(10, r = 2);
    translate([35, 10, -1])
        cylinder(10, r = 2);

    translate([40, width - 10, -1])
        cylinder(10, r = 4);
    translate([36, width - 10, -1])
        cylinder(10, r = 2);
    translate([35, width - 10, -1])
        cylinder(10, r = 2);
    
    // holes for max's

    for (i = [1 : 3]) {
        translate([height - (maxWidth * i)  - 50 + 7.8, width - 4, 8.5]) rotate([0, 90, 90])
            cylinder(5, r = 3.5);
    }
}

    // support max's
for (i = [1 : 3]) {
    translate([height - (maxWidth * i)  - 50, width - 4, 0])
        cylinder(5, r = 2.5);

    translate([height - (maxWidth * i) + 15  - 50, width - 4, 0])
        cylinder(5, r = 2.5);
}

    // support dispatcher
translate([14 + 60 + 10, 0, 0])
    cube([55, wallThickness + 5, wallThickness + 2]);
translate([14 + 60 + 10, wallThickness + 78 - 5, 0])
    cube([55, 5, wallThickness + 2]);


    // support screen
difference() {
    translate([14, (width - 99) / 2, 0]) {
        translate([0, 0, 0])
            cylinder(10, r = 4);
        
        translate([0, 99, 0])
            cylinder(10, r = 4);

        translate([60, 99, 0])
            cylinder(10, r = 4);

        translate([60, 0, 0])
            cylinder(10, r = 4);
    }

    translate([14, (width - 99) / 2, -1]) {
        translate([0, 0, 0])
            cylinder(10, r = 3);
        
        translate([0, 99, 0])
            cylinder(10, r = 3);

        translate([60, 99, 0])
            cylinder(10, r = 3);

        translate([60, 0, 0])
            cylinder(10, r = 3);
    }
    
}

// cover
color("blue")
    //translate([0, width + 20, 0])
    //translate([-2.2, 0 , 0])
    rotate([180, 0, 0]) translate([0, 10, -depth]) {
    difference() {
        translate([0, -0.2, 0]) cube([height + wallThickness + 1.5, width + 0.4, depth]);
        translate([wallThickness, -1, -1])
            cube([height + 10, width + 2, depth - wallThickness + 1]);
        
        // screen hole
        screenHoleWidth = 98;
        translate([14, (width - screenHoleWidth - 2*wallThickness)/2 + wallThickness, depth - wallThickness - 1])
            cube([59, screenHoleWidth, wallThickness * 2]);
        
            // ventilation top
        margin = 10;
        for (i = [0 : ((width - 2*margin) / 6)]){
            translate([-1, margin + (i * 6), 10])
                cube([5, 3, depth - 20]);
        }
        
        
    }
    // pattes
    /*translate([0, wallThickness + 0.4, depth - 5])
        cube([5, wallThickness, 5]);

    translate([0, width - wallThickness - wallThickness - 0.4, depth - 5])
        cube([5, wallThickness, 5]);*/

    /*translate([0, wallThickness + 0.5, wallThickness + 0.5])
        cube([5, wallThickness, 5]);

    translate([0, width - wallThickness - wallThickness - 0.5, wallThickness + 0.5])
        cube([5, wallThickness, 5]);*/

    translate([height - 10, wallThickness + 0.4, depth - 5])
        cube([5, wallThickness, 5]);

    translate([height - 10, width - wallThickness - wallThickness - 0.4, depth - 5])
        cube([5, wallThickness, 5]);
    
    // border
    translate([0, -wallThickness - 0.1, depth - 7])
        cube([height + wallThickness + 1.5, wallThickness, 7]);

    translate([0, -wallThickness - 0.1, 0])
        cube([15, wallThickness, depth]);


    translate([0, width + 0.1, depth - 7])
        cube([height + wallThickness + 1.5, wallThickness, 7]);
    translate([0, width + 0.1, 0])
        cube([15, wallThickness, depth]);
    
}

// stuff DO NOT PRINT
color("red") {

}

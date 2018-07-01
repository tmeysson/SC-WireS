Wires_Interface {
	// la fenêtre
	classvar window;
	// la grille;
	classvar grid;
	// l'ensemble des boutons créés
	classvar <instances;

	// Bus de sortie
	var <outBus;
	// bouton de contrôle
	var <knob;

	*initClass {
		instances = List();
	}

	*createNodes {|num|
		num.do {this.new};
	}

	*createInterface {|rows,cols|
		// créer une grille de rows*cols espaces vides
		grid = GridLayout.rows(nil ! rows ! cols);
		// créer la fenêtre associée
		window = Window("WireS", Rect(100,100,cols*64,rows*64)).layout_(grid).front;
		instances.do {|elt, i| elt.createElt(i, cols)};
	}

	*new {|cols|
		^super.new.intfInit;
	}

	intfInit {
		outBus = Bus.control.set(rand2(1));
		instances.add(this);
		Wires_Node.pool['scalar'].add(this);
	}

	createElt {|index, cols|
		var currow = (index / cols).floor.asInteger;
		var curcol = (index % cols).asInteger;
		knob = Knob(nil, Rect(0,0,64,64)).mode_(\horiz)
		.action_ {|v| outBus.set(v.()*2-1)};
		outBus.get {|v| {knob.value_(v+1*0.5)}.defer};
		grid.add(knob, currow, curcol);
		^this;
	}

	start {
		^this;
	}

	*destroy {
		{
			instances.do {|elt|
				elt.outBus.free;
				elt.knob.remove;
			};
			window.close;
		}.defer;
		instances = List();
	}
}
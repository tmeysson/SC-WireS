// démarrer 5 synthétiseurs en rotation
(
Wires_Def.makeDefs(true).makeWeights.addDefs;
g = Array.newClear(5);
r = Routine {
	{
		5.do {|i|
			g[i] = Wires_Node.out;
			g[i+1%5] !? (_.release);
			1.wait;
		}
	}.loop;
}.play;
)

// arrêter
r.stop; g.do(_.release);
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

// renouvellement automatique
(
Wires_Def.makeDefs(true).makeWeights.addDefs;
g = Wires_Node.out;
r = Routine {
	var num;
	{
		2.wait;
		g.renew(num = ((rand(1.0) ** 2) * (g.numNodes - 1) + 1));
		"%, %".format(num, g.numNodes).postln
	}.loop;
}.play;
)

// arrêter
r.stop; g.release;

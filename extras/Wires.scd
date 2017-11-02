/*
volume:                       0.05
nb sorties:                   3
nb noeuds audio:              10
nb noeuds controle:           30
nb noeuds remplaces par tour: 8
poids:
oscillateurs:                 3
arithmetiques:                1
constantes:                   1
sequences:                    2
*/

Server.supernova;
Server.scsynth;

// version de base en stereo
(
w = Wires(volume: 0.05, outMode: 'stereo', nbOuts: 3, nbAudio: 10, nbControl: 30,
	nbReplace: 8, typeWeights: [3,1,1,2]);
)

// version en sortie a nbChan canaux
(
var nbChan = 8;
Server.default.quit;
"SC_JACK_DEFAULT_OUTPUTS".setenv("");
Server.default.options.numOutputBusChannels = nbChan;
w = Wires(volume: 0.05, outMode: 'chan', nbOuts: nbChan, nbAudio: 10, nbControl: 30,
	nbReplace: 8, typeWeights: [3,1,1,2]);
)

w.free;

(
"N:% A:% K:%".format(
	Wires_Node.allNodes.size,
	Server.default.audioBusAllocator.blocks.size,
	Server.default.controlBusAllocator.blocks.size)
)

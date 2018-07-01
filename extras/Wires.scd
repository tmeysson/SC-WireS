// choisir un serveur
Server.supernova;
Server.scsynth;

// version de base en stereo
(
w = Wires(volume: 0.05, outMode: 'stereo', nbOuts: 3, nbAudio: 10, nbControl: 30,
	timeReplace: 2, nbReplace: 8, typeWeights: [3,1,1,2]);
)

// version avec interface
(
w = Wires(volume: 0.05, outMode: 'stereo', nbOuts: 5, nbAudio: 10, nbControl: 30,
	timeReplace: 60, nbReplace: 8, typeWeights: [3,1,1,1], intfSize: [2,3]);
)

// version en sortie a nbChan canaux
(
var nbChan = 8;
Server.default.quit;
"SC_JACK_DEFAULT_OUTPUTS".setenv(("ardour:Audio _/audio_in 1")!nbChan);
Server.default.options.numOutputBusChannels = nbChan;
w = Wires(volume: 0.05, outMode: 'chan', nbOuts: nbChan, nbAudio: 10, nbControl: 30,
	timeReplace: 8, nbReplace: 8, typeWeights: [3,1,1,2]);
)

w.free;

(
"N:% A:% K:%".format(
	Wires_Node.allNodes.size,
	Server.default.audioBusAllocator.blocks.size,
	Server.default.controlBusAllocator.blocks.size)
)

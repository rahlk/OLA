import sys
import ipdb
import networkx as nx
import matplotlib.pyplot as plt
import pydot

# This script reads a csv file of the format:
# fromContext fromVar toContext toVar
# and creates a graph of the flow of values, using as edges:
# fromVar@fromContext -> toVar@toContext
# Some methods are provided below to help navigate the graph

# might need: pip install --user ipdb networkx

G = nx.DiGraph()
#plt.subplot(121)
print "reading files..."
application_method_set = set()

with open(sys.argv[1], 'r') as f:
    for i in f.read().strip().split("\n"):
        application_method_set.add(i)

print("\n\n Application Methods")
print("-"*16)
print(application_method_set)

nodes = lambda: G.nodes()
all_paths = lambda: nx.all_pairs_shortest_path(G)
path_from = lambda x: nx.shortest_path(G, x)
path_to = lambda x: nx.shortest_path(G, target=x)
exist = lambda x, y: nx.has_path(G, x, y)
path = lambda x, y: nx.shortest_path(G, x, y)
grep = lambda txt, flt: [x for x in txt if all([(y in x) for y in [[flt], flt][type(flt) is list]])]
print_graph = lambda: nx.draw(G, with_labels=True, font_weight='bold')

#print("\n\nData read")
#print("-"*12)
with open(sys.argv[2], 'r') as f:
    for i in f.read().strip().split("\n"):
        x = i.split("/")
        #print(x)
        if x[0] in application_method_set:
            #print(x[0])
            frm = x[0]
            to = x[1]
            G.add_path([frm, to], title = x[2])
    edge_labels = nx.get_edge_attributes(G, 'title')
    nx.draw_spring(G, with_labels=True, font_weight='bold')

    if len(sys.argv) > 3 and sys.argv[3] == '1':
        pos = nx.spring_layout(G)
        nx.draw_networkx_edge_labels(G, pos, edge_labels=edge_labels)

    plt.show()
    plt.savefig('irs_call_graph_with_context.png')
    plt.clf()
    #graph = nx.drawing.nx_pydot.to_pydot(G)
    #graph.write_png('call_graph.png')

def read_taint_flow_vars(x):
    with open(x) as f:
        flows = [['{}@{}'.format(z[1],z[0]), '{}@{}'.format(z[3],z[2])] for z in [y.split("\t") for y in f.read().strip().split("\n")]]
        flows = [x for x in flows if G.has_node(x[0]) and G.has_node(x[1])]
        print(flows)
        return flows

vars_to_flows = lambda x: sorted([path(*y) for y in x if exist(*y)], key=len)

print "nodes() returns all nodes in the graph"
print "all_paths() returns all paths"
print "path_from(x) returns paths from x"
print "path_to(x) returns paths to x"
print "path(x, y) returns paths from x to y"
print "exist(x, y) returns if a path exists from x to y"
print "grep(x, y) returns strings of list x that contain all strings in list y (y can also be a string)"
print "read_taint_flow_vars(f) reads file f (should be in LeakingTaintedInformationVars.csv form) and returns a list of from-to for leaking information pairs"
print "vars_to_flows(x) takes a list of from-to pairs and returns the flows for these vars sorted by length"
print "print_graph() print graph"

flows = None
if len(sys.argv) > 4:
    fp = sys.argv[4]
    print "Reading `flows` from: " + fp
    flows = vars_to_flows(read_taint_flow_vars(fp))

#ipdb.set_trace()

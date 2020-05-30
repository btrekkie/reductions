# reductions
This is a Java program that implements reductions demonstrating that certain
problems are hard.  Specifically, it demonstrates that Mario, Zelda,
Pok&eacute;mon, Push-1, and PushPush are NP-hard using reductions from 3-SAT.
This program also implements algorithms that are actually useful, including
planarity testing, planar embedding, and graph drawing algorithms.  It is tested
in Java 7.0, but I think it probably works in Java 5.0 and higher.

# Legitimately useful features
This project includes implementations of the following algorithms, each of which
takes linear time assuming constant amortized `HashMap` / `HashSet` performance:

* Testing whether a graph is planar.  (A graph is planar if it has a planar
  drawing, which assigns each vertex a 2D point and draws the edges as
  non-crossing curves in the plane.)
* Computing a planar embedding for a planar graph.  (A planar embedding is a
  clockwise ordering of the edges around each vertex and an external face that
  are feasible in a planar drawing.)
* Computing a weak visibility representation for a planar graph.  (A weak
  visibility representation is a drawing of a planar graph where vertices are
  horizontal line segments and edges are vertical line segments, none of which
  cross.)
* Computing an SPQR tree.  (See
  [src/com/github/btrekkie/graph/spqr/SpqrNode.java](src/com/github/btrekkie/graph/spqr/SpqrNode.java)
  for a description of SPQR trees.)
* Computing an EC-planar embedding, and testing for EC-planarity.  An EC-planar
  embedding is a planar embedding with certain constraints on the clockwise
  ordering of the edges around the vertices.
* Computing a block-cut tree.  (See
  [src/com/github/btrekkie/graph/bc/BlockNode.java](src/com/github/btrekkie/graph/bc/BlockNode.java)
  for a description of block-cut trees.)
* Computing a dual graph.  (The dual H of a graph G is a graph with one vertex
  for each face in G, including the external face, and an edge between each pair
  of vertices corresponding to two faces in G separated by an edge.)
* Augmenting a planar graph to be biconnected while preserving a planar
  embedding.  (A graph is biconnected if it remains connected if we remove any
  vertex.  Augmentation is the addition of edges.)

It also includes the following features:

* Computing a set of edge crossings to add to a graph to make it planar or
  EC-planar.  I haven't done a good analysis of the number of crossings the
  algorithm produces, but in the interest of full disclosure, I will say I get
  the sense that it's rather large.
* A Swing JComponent and JFrame for displaying images rendered programatically.
  This is necessary for the reductions because some of the stages they produce
  are so large that storing the results as an image file is impractical.
  Instead, the program renders the stages at the desired position and level of
  zoom on the fly.

# Fun features
This program demonstrates that the original Super Mario Bros., the original
Legend of Zelda, all versions of Pok&eacute;mon, and the Push-1 and PushPush
games are NP-hard using reductions from 3-SAT.  In each case, the problem is
whether it is possible to get from here to there.  For an explanation of the
term "NP-hard", see [the Wikipedia article on P vs.
NP](https://en.wikipedia.org/wiki/P_versus_NP_problem).  Basically, any program
that determines whether it is possible to beat a level in one of the
aforementioned games must take a long time to run, in a certain formal sense.
(This is assuming the unproven but widely believed claim that P &ne; NP.)  For a
definition of 3-SAT, see
[src/com/github/btrekkie/reductions/bool/ThreeSat.java](src/com/github/btrekkie/reductions/bool/ThreeSat.java).

# Demo
For a demonstration of the Mario reduction, run the following UNIX commands from
the root project directory:

<pre>
mkdir bin
javac -sourcepath src `find . -name "*.java" | grep -v Test` -d bin
java -cp bin com.github.btrekkie.reductions.mario.MarioProblem
</pre>

A window displaying a Mario level should appear.  The level takes a while to
render.  For faster results, click the "+" button 15 to 20 times.  To see
demonstrations of the Zelda, Pok&eacute;mon, or Push-1 / PushPush reductions,
change `mario.MarioProblem` in the last command to `zelda.ZeldaProblem`,
`pokemon.PokemonProblem`, or `push1.Push1Problem` respectively.

Pok&eacute;mon problems feature strong trainers, who always defeat the player,
and weak trainers, who never defeat the player.  The sight lines of the strong
trainers are show in blue, and the sight lines of the weak trainers are shown in
red.

The gadgets and reduction structure are taken from [Aloupis, Demaine, Guo,
Viglietta (2012): Classic Nintendo Games are (Computationally)
Hard](https://arxiv.org/pdf/1203.1895.pdf) and [Demaine, Demaine, and O'Rourke
(2000): PushPush and Push-1 are NP-hard in
2D](https://arxiv.org/pdf/cs/0007021v2.pdf).  See those papers for a more
detailed description of what is going on in the stages this program produces.

# Documentation
See <https://btrekkie.github.io/reductions/index.html> for API documentation.

# Credits
* Super Mario Bros., The Legend of Zelda, and Pok&eacute;mon are &copy;
  Nintendo.
* The Super Mario Bros. sprite sheet was composed by Beam Luinsir Yosho.
* The Legend of Zelda sprites are from [The Spriters
  Resource](https://www.spriters-resource.com/), ripped by Alien Link and by an
  unknown contributor.
* The algorithms used for the reductions and for various graph drawing problems
  are taken or adapted from different papers and notes, credited in the comments
  for the respective classes and methods.

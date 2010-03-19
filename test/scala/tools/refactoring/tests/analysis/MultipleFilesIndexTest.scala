package scala.tools.refactoring.tests.analysis

import scala.tools.refactoring.analysis.FullIndexes
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.tools.refactoring.tests.util.TestHelper
import org.junit.{Test, Before}
import junit.framework.TestCase
import org.junit.Assert._
import scala.tools.refactoring.common.Selections
import scala.tools.refactoring.analysis.{Indexes, TreeAnalysis}
import scala.tools.nsc.ast.Trees
import scala.tools.nsc.util.{SourceFile, BatchSourceFile, RangePosition}

@Test
class MultipleFilesIndexTest extends TestHelper with FullIndexes with TreeAnalysis {

  import global._

  def findReferences(pro: FileSet): List[String] = {
              
    val sym = pro.selection.selectedSymbols head
    
    pro.trees foreach index.processTree

    def aggregateFileNamesWithTrees[T <: Tree](ts: List[T])(conversion: T => String) = {
      new HashMap[String, ListBuffer[T]] {
        ts foreach {ref => 
          getOrElseUpdate(ref.pos.source.file.name, new ListBuffer[T]) += ref
        }
      }.toList.sortWith(_._1 < _._1).unzip._2 map (_ filter (_.pos.isRange) map conversion sortWith(_ < _) mkString ", ")
    }
    
    aggregateFileNamesWithTrees(index occurences sym) { symTree => 
      symTree.symbol.nameString +" on line "+ symTree.pos.line   
    }
  }
  
  @Test
  def findReferencesToClass = new FileSet("p1") {

    add(
    """
      package p1
      /*(*/  abstract class  A  /*)*/
      class C extends A
    """,
    """A on line 3, A on line 4""")
    
    add(
    """
      package p1
      class B extends A {

        val a = new p1.A { }
      }
    """,
    """A on line 3, A on line 5""")
    
    add(
    """
      package p1.subpackage
      class C extends p1.A {
        val as = new scala.collection.mutable.ListBuffer[p1.A]
        val ass: List[p1.A]
      }

      class X(a: p1.A)
    """,
    """A on line 3, A on line 4, A on line 5, A on line 8""")
    
  } apply(findReferences)
  
  @Test
  def findReferencesToMethod = new FileSet("p2") {

    add(
    """
      package p2
      class A {
        /*(*/  def method() = "hello world"  /*)*/
      }

      class C {
        (new A).method()
      }
    """,
    """method on line 4, method on line 8""")
    
    add(
    """
      package p2
      class D(a: A) {
        val am = a.method()
      }
    """,
    """method on line 4""")
    
    add(
    """
      package p2
      class E extends A {
        val n = method()
        val m = super.method()
        override def method() = "hello 2"
      }

      class F(e: E) {
        val x = e.method()
      }

      class G extends A
    """,
    """method on line 10, method on line 4, method on line 5, method on line 6""")
    
  } apply(findReferences)
  
  @Test
  def findReferencesToTraitMethod = new FileSet("p3") {

    add(
    """
      package p3
      trait Trait {
        /*(*/  def method: String  /*)*/
      }
      
      class A extends Trait {
        def method = "impl by def"
      }

      class B extends Trait {
        val method = "impl by val"
      }
    """,
    """method on line 12, method on line 4, method on line 8""")
    
    add(
    """
      package p3.subpackage
      class C(val method: String) extends p3.Trait
    """,
    """method on line 3""")
    
  } apply(findReferences)
  
  @Test
  def findReferencesFromCallSite = new FileSet("p4") {

    add(
    """
      package p4
      object O {
        def getInt = 42
      }
    """,
    """getInt on line 4""")
    
    add(
    """
      package p4
      class A {
        val int = /*(*/  O.getInt  /*)*/
      }
    """,
    """getInt on line 4""")
    
  } apply(findReferences)
  
  @Test
  def findValues = new FileSet("p5") {

    add(
    """
      package p5
      class A(val s: String) {
        val ss = /*(*/  s  /*)*/
      }
    """,
    """s on line 3, s on line 4""")

    add(
    """
      package p5
      class B(override val s: String) 
          extends A(s) {
        val b = new B("b")
        val bb = b.s
      }
    """,
    """s on line 3, s on line 4, s on line 6""")

  } apply(findReferences)  

  @Test
  def findSuperCall = new FileSet("p6") {

    add(
    """
      package p6
      class A(/*(*/  val s: String  /*)*/, val t: Int)
      class B(override val s: String) 
        extends A(s, 5)
    """,
    """s on line 3, s on line 4, s on line 5""")
  } apply(findReferences)
  
  @Test
  def findCaseClassValues = new FileSet("p7") {

    add(
    """
      package p7
      case class A(/*(*/  s: String  /*)*/)
      object B {
        val a = A("hello").s
      }
    """,
    """s on line 3, s on line 5""")
  } apply(findReferences)
  
  @Test
  def passMethodAsFunction = new FileSet("p8") {

    add(
    """
      package p8
      class A {
        /*(*/  def double(i: Int) = i * 2  /*)*/
      }
      object Main {
        val a = new A
        List(1,2,3) map a.double 
      }
    """,
    """double on line 4, double on line 8""")
  } apply(findReferences)
}

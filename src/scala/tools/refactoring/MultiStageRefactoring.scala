package scala.tools.refactoring

import scala.tools.nsc.interactive.Global

abstract class MultiStageRefactoring(global: Global) extends Refactoring(global) {
 
  type PreparationResult
  
  case class PreparationError(val cause: String)
  
  case class RefactoringError(val cause: String)
  
  type RefactoringParameters
 
  def prepare(s: Selection): Either[PreparationError, PreparationResult]
  
  def perform(selection: Selection, prepared: PreparationResult, params: RefactoringParameters): Either[RefactoringError, TreeModifications]

}
package cc.factorie.app.nlp.parse.nonproj

import cc.factorie.app.classify.LogLinearTemplate2
import cc.factorie.app.classify.ModelBasedClassifier
import cc.factorie.app.classify.SVMTrainer
import cc.factorie.app.classify.LabelList
import cc.factorie.TemplateModel
import cc.factorie.Model
import ParserSupport._
import ParserConstants._
import java.io.File
import cc.factorie.app.nlp.Document
import cc.factorie.app.nlp.Sentence
import cc.factorie.app.nlp.parse.ParseTree
import cc.factorie.app.nlp.parse.ParseTreeLabel
import cc.factorie.app.nlp.parse.ParseTreeLabelDomain
import cc.factorie._

object ParserSVM {
  
  def main(args: Array[String]): Unit = {
    val trainer = new ParserSVM()
    trainer.run(args)
  }
}

class ParserSVM extends Parser with ParserImpl {
  
  def getEmptyModel(): TemplateModel = new TemplateModel(new LogLinearTemplate2[ParseDecisionVariable, NonProjDependencyParserFeatures](lTof, DecisionDomain, NonProjParserFeaturesDomain))

  def save(c: ParserClassifier, folder: File, gzip: Boolean) { save(c, folder, gzip, false) }

  def load(file: File, gzip: Boolean) = load(file, gzip, false)

  def load(folder: File, gzip: Boolean, doubleGzip: Boolean): ParserClassifier = {
    val c = new BaseParserClassifier(new ModelBasedClassifier(getEmptyModel(), DecisionDomain))
    c.load(folder, gzip, doubleGzip)
    c
  }
  
  def save(c: ParserClassifier, folder: File, gzip: Boolean, doubleGzip: Boolean): Unit = c.asInstanceOf[BaseParserClassifier].save(folder, gzip, doubleGzip)
  
  def train[B <: ParseDecisionVariable](vs: Seq[B]): ParserClassifier = {
    val backingClassifier = (new SVMTrainer(parallel = true)).train(new LabelList[ParseDecisionVariable, NonProjDependencyParserFeatures](vs, lTof)).asInstanceOf[ModelBasedClassifier[ParseDecisionVariable, Model with Weights]]
    new BaseParserClassifier(backingClassifier)
  }
  
  def process(c: ParserClassifier, doc: Document): Document = {
    doc.sentences.foreach(process(c, _))
    doc
  }

  def process(c: ParserClassifier, s: Sentence): Sentence = {
    val p = new ParserAlgorithm(mode = PREDICTING)
    p.predict = c.classify

    val parse = new ParseTree(s)
    p.parse(s).drop(1).filter(_.hasHead).map { dt => 
      parse.setParent(dt.thisIdx - 1, dt.head.depToken.thisIdx - 1)
      // TODO: why is this necessary? Shouldn't I be able to do set(s: String)?
      parse.label(dt.thisIdx - 1).set(ParseTreeLabelDomain.index(dt.head.label))(null)
    }
    s.attr += parse
    s
  }

}
package jobs

import jobs.steps.OpenHighDimensionalDataStep
import jobs.steps.ParametersFileStep
import jobs.steps.RCommandsStep
import jobs.steps.Step
import jobs.steps.helpers.NumericColumnConfigurator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.projections.Projection

import static jobs.AbstractAnalysisJob.PARAM_ANALYSIS_CONSTRAINTS

abstract class HighDimensionalOnlyJob extends AbstractAnalysisJob {

    @Autowired
    HighDimensionResource highDimensionResource

    @Autowired
    ApplicationContext appCtx

    private void configure() {
        def dependentConfigurator   = appCtx.getBean NumericColumnConfigurator
        def independentConfigurator = appCtx.getBean NumericColumnConfigurator

        dependentConfigurator = new NumericColumnConfigurator(
                columnHeader: 'X',
                projection: Projection.DEFAULT_REAL_PROJECTION,
                keyForConceptPath: 'dependentVariable',
                keyForDataType: 'divDependentVariableType',
                keyForSearchKeywordId: 'divDependentVariablePathway')
        independentConfigurator = new NumericColumnConfigurator(
                columnHeader: 'Y',
                projection: Projection.DEFAULT_REAL_PROJECTION,
                keyForConceptPath: 'independentVariable',
                keyForDataType: 'divIndependentVariableType',
                keyForSearchKeywordId: 'divIndependentVariablePathway')

        dependentConfigurator.addColumn()
        independentConfigurator.addColumn()
    }

    protected List<Step> prepareSteps() {
        List<Step> steps = []

        steps << new ParametersFileStep(
                temporaryDirectory: temporaryDirectory,
                params: params)

        steps
        def openResultSetStep = new OpenHighDimensionalDataStep(
                params: params,
                dataTypeResource: highDimensionResource.getSubResourceForType(
                        params[PARAM_ANALYSIS_CONSTRAINTS]['data_type']))

        steps << openResultSetStep

        steps << createDumpHighDimensionDataStep { -> openResultSetStep.results }

        steps << new RCommandsStep(
                temporaryDirectory: temporaryDirectory,
                scriptsDirectory: scriptsDirectory,
                rStatements: RStatements,
                studyName: studyName,
                params: params)

        steps
    }

    abstract protected Step createDumpHighDimensionDataStep(Closure resultsHolder)

}

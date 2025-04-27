package cloudwatch

import (
	"context"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/cloudwatch"
	"github.com/aws/aws-sdk-go-v2/service/cloudwatch/types"
)

var cw cloudwatch.Client

func InitCloudWatch(ctx context.Context) {
	cfg, err := config.LoadDefaultConfig(ctx, config.WithRegion("us-east-1"))
	if err != nil {
		panic("unable to load SDK config, " + err.Error())
	}
	cw = *cloudwatch.NewFromConfig(cfg)
}

func PutMetric(ctx context.Context, metricName string, value float64, namespace string) {
	_, err := cw.PutMetricData(ctx, &cloudwatch.PutMetricDataInput{
		Namespace: &namespace,
		MetricData: []types.MetricDatum{
			{
				MetricName: aws.String(metricName),
				Value:      aws.Float64(1.0),
				Unit:       types.StandardUnitCount,
				Dimensions: []types.Dimension{},
			},
		},
	})

	if err != nil {
		panic(err)
	}
}

output "bucket_name" {
  description = "S3 bucket name for the landing page static files"
  value       = aws_s3_bucket.landing.bucket
}

output "cloudfront_domain" {
  description = "CloudFront distribution domain name"
  value       = aws_cloudfront_distribution.landing.domain_name
}

output "cloudfront_distribution_id" {
  description = "CloudFront distribution ID (for cache invalidation)"
  value       = aws_cloudfront_distribution.landing.id
}

output "landing_url" {
  description = "Public URL for the landing page"
  value       = local.has_domain ? "https://${var.domain_name}" : "https://${aws_cloudfront_distribution.landing.domain_name}"
}

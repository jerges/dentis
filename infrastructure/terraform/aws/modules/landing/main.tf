locals {
  has_domain = var.domain_name != "" && var.certificate_arn != ""
}

# ─── S3 bucket for static files ──────────────────────────────────────────────

resource "aws_s3_bucket" "landing" {
  bucket        = "${var.name_prefix}-landing"
  force_destroy = true
  region        = var.aws_region
  tags = { Name = "${var.name_prefix}-landing" }
}

resource "aws_s3_bucket_public_access_block" "landing" {
  bucket                  = aws_s3_bucket.landing.id
  region                  = var.aws_region
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "landing" {
  bucket = aws_s3_bucket.landing.id
  region = var.aws_region
  versioning_configuration { status = "Enabled" }
}

# ─── CloudFront Origin Access Control ────────────────────────────────────────

resource "aws_cloudfront_origin_access_control" "landing" {
  name                              = "${var.name_prefix}-landing-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# ─── Bucket policy (allow CloudFront OAC only) ───────────────────────────────

data "aws_iam_policy_document" "landing_bucket" {
  statement {
    sid    = "AllowCloudFrontOAC"
    effect = "Allow"
    actions = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.landing.arn}/*"]
    principals {
      type = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "aws:SourceArn"
      values = [aws_cloudfront_distribution.landing.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "landing" {
  bucket = aws_s3_bucket.landing.id
  region = var.aws_region
  policy = data.aws_iam_policy_document.landing_bucket.json
}

# ─── CloudFront distribution ─────────────────────────────────────────────────

resource "aws_cloudfront_distribution" "landing" {
  enabled             = true
  default_root_object = "index.html"
  http_version        = "http2and3"
  price_class = "PriceClass_100" # US, Canada, Europe
  comment             = "${var.name_prefix} landing page"

  origin {
    domain_name              = aws_s3_bucket.landing.bucket_regional_domain_name
    origin_id                = "s3-landing"
    origin_access_control_id = aws_cloudfront_origin_access_control.landing.id
  }

  default_cache_behavior {
    target_origin_id       = "s3-landing"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods = ["GET", "HEAD", "OPTIONS"]
    cached_methods = ["GET", "HEAD"]
    compress               = true

    forwarded_values {
      query_string = false
      cookies { forward = "none" }
    }

    min_ttl = 0
    default_ttl = 86400  # 24h
    max_ttl = 604800 # 7d
  }

  # SPA fallback — serve index.html for 403/404
  custom_error_response {
    error_code            = 403
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 10
  }

  custom_error_response {
    error_code            = 404
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 10
  }

  aliases = local.has_domain ? [var.domain_name] : []

  restrictions {
    geo_restriction { restriction_type = "none" }
  }

  viewer_certificate {
    acm_certificate_arn            = local.has_domain ? var.certificate_arn : null
    ssl_support_method             = local.has_domain ? "sni-only" : null
    minimum_protocol_version       = local.has_domain ? "TLSv1.2_2021" : "TLSv1"
    cloudfront_default_certificate = !local.has_domain
  }

  tags = { Name = "${var.name_prefix}-landing-cf" }
}

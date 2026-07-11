/// # Codebuild
/// > Provision the CI/CD pipeline that checks out the website repository and publishes it to the website bucket.
///
/// ## Boundary
/// - `provision-publishing-pipeline` — create the pipeline that checks out the website repository and runs the publish stage against the website bucket and its content distribution
///
/// ## Requirements
/// ### R1: Publish only the public directory
/// - R1.1 — When the publish stage runs, the BC shall publish exclusively the contents of the checkout's `public` directory to the website bucket. _(why: the repository root carries sources and meta files that must never reach the public site)_
/// - R1.2 — If the checkout contains no `public` directory, then the BC shall fail the publish stage before modifying the website bucket.
/// - R1.3 — While the `public` directory is present, the BC shall empty the website bucket before copying, so previously published files that no longer exist do not linger.
/// - R1.4 — When the copy has completed, the BC shall invalidate the content distribution.
///
/// ## Entities
/// - WebsiteBuildConfiguration
///
/// ## Out of scope
/// - Building or generating the `public` directory — the website repository owns its own build.
/// - Provisioning the website bucket and the content distribution (owned by the `s3` and `cloudfront` BCs).
package airhacks.website.codebuild;

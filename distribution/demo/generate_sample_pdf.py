#!/usr/bin/env python3
"""Generate a sample PDF document for JSignPdf screenshots."""

from fpdf import FPDF

# Colors
DARK_BLUE = (20, 50, 90)
MEDIUM_BLUE = (40, 80, 140)
LIGHT_BLUE = (220, 232, 245)
ACCENT_BLUE = (60, 120, 190)
DARK_GRAY = (50, 50, 50)
MEDIUM_GRAY = (100, 100, 100)
LIGHT_GRAY = (200, 200, 200)
WHITE = (255, 255, 255)


class SamplePDF(FPDF):
    def header(self):
        pass

    def footer(self):
        self.set_y(-15)
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(*MEDIUM_GRAY)
        self.cell(0, 10, f"Page {self.page_no()}/{{nb}}", align="C")

    def draw_header_bar(self):
        # Dark blue header bar
        self.set_fill_color(*DARK_BLUE)
        self.rect(0, 0, 210, 38, "F")

        # Accent line
        self.set_fill_color(*ACCENT_BLUE)
        self.rect(0, 38, 210, 1.5, "F")

        # Title text
        self.set_xy(15, 8)
        self.set_font("Helvetica", "B", 22)
        self.set_text_color(*WHITE)
        self.cell(0, 10, "SERVICE AGREEMENT", ln=True)

        # Subtitle
        self.set_x(15)
        self.set_font("Helvetica", "", 10)
        self.set_text_color(180, 200, 220)
        self.cell(0, 8, "Professional Services Contract  |  Agreement No. SA-2026-0042")

    def draw_info_box(self):
        self.set_y(48)
        # Light blue info box
        self.set_fill_color(*LIGHT_BLUE)
        self.set_draw_color(*ACCENT_BLUE)
        self.rect(15, 46, 180, 36, "DF")

        # Left column - Provider
        self.set_xy(20, 48)
        self.set_font("Helvetica", "B", 8)
        self.set_text_color(*MEDIUM_BLUE)
        self.cell(80, 5, "SERVICE PROVIDER")
        self.set_xy(20, 53)
        self.set_font("Helvetica", "", 9)
        self.set_text_color(*DARK_GRAY)
        self.cell(80, 5, "Acme Consulting Group, LLC")
        self.set_xy(20, 58)
        self.set_font("Helvetica", "", 8)
        self.set_text_color(*MEDIUM_GRAY)
        self.cell(80, 5, "123 Innovation Drive, Tech Park")
        self.set_xy(20, 63)
        self.cell(80, 5, "San Francisco, CA 94105")

        # Vertical separator
        self.set_draw_color(*ACCENT_BLUE)
        self.line(105, 49, 105, 78)

        # Right column - Client
        self.set_xy(110, 48)
        self.set_font("Helvetica", "B", 8)
        self.set_text_color(*MEDIUM_BLUE)
        self.cell(80, 5, "CLIENT")
        self.set_xy(110, 53)
        self.set_font("Helvetica", "", 9)
        self.set_text_color(*DARK_GRAY)
        self.cell(80, 5, "Global Enterprises, Inc.")
        self.set_xy(110, 58)
        self.set_font("Helvetica", "", 8)
        self.set_text_color(*MEDIUM_GRAY)
        self.cell(80, 5, "456 Corporate Boulevard, Suite 800")
        self.set_xy(110, 63)
        self.cell(80, 5, "New York, NY 10001")

        # Date and effective info
        self.set_xy(20, 70)
        self.set_font("Helvetica", "B", 8)
        self.set_text_color(*MEDIUM_BLUE)
        self.cell(40, 5, "Effective Date:")
        self.set_font("Helvetica", "", 8)
        self.set_text_color(*DARK_GRAY)
        self.cell(40, 5, "April 16, 2026")

        self.set_xy(110, 70)
        self.set_font("Helvetica", "B", 8)
        self.set_text_color(*MEDIUM_BLUE)
        self.cell(40, 5, "Expiration Date:")
        self.set_font("Helvetica", "", 8)
        self.set_text_color(*DARK_GRAY)
        self.cell(40, 5, "April 15, 2027")

    def section_heading(self, number, title):
        self.ln(4)
        self.set_font("Helvetica", "B", 11)
        self.set_text_color(*DARK_BLUE)

        # Small accent bar before heading
        y = self.get_y()
        self.set_fill_color(*ACCENT_BLUE)
        self.rect(15, y + 1, 3, 5, "F")

        self.set_x(21)
        self.cell(0, 7, f"{number}. {title}", ln=True)
        self.ln(1)

    def body_text(self, text):
        self.set_x(15)
        self.set_font("Helvetica", "", 9)
        self.set_text_color(*DARK_GRAY)
        self.multi_cell(180, 4.5, text)
        self.ln(1)

    def bullet_point(self, text):
        self.set_x(22)
        self.set_font("Helvetica", "", 9)
        self.set_text_color(*DARK_GRAY)
        bullet_x = self.get_x()
        bullet_y = self.get_y()
        # Draw bullet dot
        self.set_fill_color(*ACCENT_BLUE)
        self.ellipse(bullet_x - 4, bullet_y + 1.2, 2, 2, "F")
        self.multi_cell(170, 4.5, text)
        self.ln(0.5)

    def draw_table(self):
        """Draw a payment schedule table."""
        headers = ["Milestone", "Description", "Due Date", "Amount"]
        col_widths = [25, 80, 35, 40]

        # Table header
        self.set_x(15)
        self.set_font("Helvetica", "B", 8)
        self.set_fill_color(*DARK_BLUE)
        self.set_text_color(*WHITE)
        for i, header in enumerate(headers):
            self.cell(col_widths[i], 7, header, border=1, fill=True, align="C")
        self.ln()

        # Table rows
        rows = [
            ["Phase 1", "Project Initiation & Planning", "May 15, 2026", "$12,500.00"],
            ["Phase 2", "Development & Implementation", "Jul 30, 2026", "$25,000.00"],
            ["Phase 3", "Testing & Quality Assurance", "Sep 15, 2026", "$10,000.00"],
            ["Phase 4", "Deployment & Training", "Nov 1, 2026", "$12,500.00"],
        ]

        self.set_font("Helvetica", "", 8)
        for i, row in enumerate(rows):
            self.set_x(15)
            if i % 2 == 0:
                self.set_fill_color(*LIGHT_BLUE)
            else:
                self.set_fill_color(*WHITE)
            self.set_text_color(*DARK_GRAY)
            aligns = ["C", "L", "C", "R"]
            for j, cell in enumerate(row):
                self.cell(col_widths[j], 6, cell, border=1, fill=True, align=aligns[j])
            self.ln()

        # Total row
        self.set_x(15)
        self.set_font("Helvetica", "B", 8)
        self.set_fill_color(230, 238, 248)
        self.set_text_color(*DARK_BLUE)
        self.cell(140, 7, "TOTAL", border=1, fill=True, align="R")
        self.cell(40, 7, "$60,000.00", border=1, fill=True, align="R")
        self.ln()

    def draw_signature_block(self):
        """Draw signature lines at the bottom."""
        y = self.get_y() + 5

        # Light background for signature area
        self.set_fill_color(248, 250, 252)
        self.set_draw_color(*LIGHT_GRAY)
        self.rect(15, y, 180, 42, "DF")

        self.set_y(y + 3)
        self.set_x(15)
        self.set_font("Helvetica", "B", 9)
        self.set_text_color(*DARK_BLUE)
        self.cell(180, 5, "SIGNATURES", align="C", ln=True)
        self.ln(2)

        sig_y = self.get_y()

        # Left signature
        self.set_draw_color(*DARK_BLUE)
        self.line(25, sig_y + 18, 95, sig_y + 18)
        self.set_xy(25, sig_y + 19)
        self.set_font("Helvetica", "B", 8)
        self.set_text_color(*DARK_GRAY)
        self.cell(70, 4, "John Smith, Managing Director")
        self.set_xy(25, sig_y + 23)
        self.set_font("Helvetica", "", 8)
        self.set_text_color(*MEDIUM_GRAY)
        self.cell(70, 4, "Acme Consulting Group, LLC")

        # Right signature
        self.line(115, sig_y + 18, 185, sig_y + 18)
        self.set_xy(115, sig_y + 19)
        self.set_font("Helvetica", "B", 8)
        self.set_text_color(*DARK_GRAY)
        self.cell(70, 4, "Jane Doe, Chief Executive Officer")
        self.set_xy(115, sig_y + 23)
        self.set_font("Helvetica", "", 8)
        self.set_text_color(*MEDIUM_GRAY)
        self.cell(70, 4, "Global Enterprises, Inc.")

        # Date fields
        self.set_xy(25, sig_y + 28)
        self.set_font("Helvetica", "", 8)
        self.set_text_color(*MEDIUM_GRAY)
        self.cell(30, 4, "Date: ____________")
        self.set_xy(115, sig_y + 28)
        self.cell(30, 4, "Date: ____________")


def generate():
    pdf = SamplePDF()
    pdf.alias_nb_pages()
    pdf.set_auto_page_break(auto=True, margin=20)

    # --- Page 1 ---
    pdf.add_page()
    pdf.draw_header_bar()
    pdf.draw_info_box()

    pdf.set_y(88)

    pdf.section_heading("1", "PURPOSE AND SCOPE")
    pdf.body_text(
        "This Service Agreement (the \"Agreement\") is entered into by and between "
        "Acme Consulting Group, LLC (\"Provider\") and Global Enterprises, Inc. (\"Client\"), "
        "collectively referred to as the \"Parties.\" This Agreement sets forth the terms and "
        "conditions under which the Provider shall deliver professional consulting and "
        "technology implementation services to the Client."
    )
    pdf.body_text(
        "The scope of services includes, but is not limited to, the following deliverables:"
    )
    pdf.bullet_point("Strategic assessment and technology roadmap development")
    pdf.bullet_point("Custom software development and system integration services")
    pdf.bullet_point("Data migration, quality assurance, and performance testing")
    pdf.bullet_point("End-user training, documentation, and knowledge transfer")
    pdf.bullet_point("Post-deployment support and maintenance for a period of 90 days")

    pdf.section_heading("2", "TERM AND TERMINATION")
    pdf.body_text(
        "This Agreement shall commence on the Effective Date and remain in force for a "
        "period of twelve (12) months, unless terminated earlier in accordance with the "
        "provisions herein. Either Party may terminate this Agreement by providing sixty "
        "(60) days' written notice to the other Party. In the event of a material breach, "
        "the non-breaching Party may terminate immediately upon written notice."
    )

    pdf.section_heading("3", "COMPENSATION AND PAYMENT TERMS")
    pdf.body_text(
        "The Client agrees to compensate the Provider according to the following milestone-based "
        "payment schedule. All invoices are due within thirty (30) days of receipt. Late payments "
        "shall accrue interest at a rate of 1.5% per month."
    )
    pdf.ln(2)
    pdf.draw_table()

    pdf.section_heading("4", "CONFIDENTIALITY")
    pdf.body_text(
        "Each Party agrees to maintain the confidentiality of all proprietary and sensitive "
        "information disclosed during the course of this engagement. Confidential information "
        "includes, but is not limited to, trade secrets, business strategies, technical "
        "specifications, customer data, and financial records. This obligation shall survive "
        "the termination of this Agreement for a period of three (3) years."
    )

    # --- Page 2 ---
    pdf.add_page()

    # Subtle header on continuation pages
    pdf.set_fill_color(*DARK_BLUE)
    pdf.rect(0, 0, 210, 12, "F")
    pdf.set_fill_color(*ACCENT_BLUE)
    pdf.rect(0, 12, 210, 0.8, "F")
    pdf.set_xy(15, 2)
    pdf.set_font("Helvetica", "B", 9)
    pdf.set_text_color(*WHITE)
    pdf.cell(130, 8, "SERVICE AGREEMENT  |  SA-2026-0042")
    pdf.set_font("Helvetica", "", 8)
    pdf.set_text_color(180, 200, 220)
    pdf.cell(50, 8, "Page 2 of 2", align="R")

    pdf.set_y(20)

    pdf.section_heading("5", "INTELLECTUAL PROPERTY")
    pdf.body_text(
        "All intellectual property, deliverables, and work product created by the Provider "
        "in the performance of this Agreement shall become the exclusive property of the Client "
        "upon full payment of all fees. The Provider retains the right to use general knowledge, "
        "skills, and experience gained during the engagement, provided such use does not "
        "disclose the Client's confidential information."
    )

    pdf.section_heading("6", "WARRANTIES AND REPRESENTATIONS")
    pdf.body_text(
        "The Provider warrants that all services shall be performed in a professional and "
        "workmanlike manner, consistent with industry standards. The Provider further represents "
        "that it has the necessary expertise, resources, and authority to fulfill its obligations "
        "under this Agreement. The Client warrants that it shall provide timely access to "
        "necessary systems, personnel, and information required for the Provider to perform "
        "the services described herein."
    )

    pdf.section_heading("7", "LIMITATION OF LIABILITY")
    pdf.body_text(
        "In no event shall either Party be liable to the other for any indirect, incidental, "
        "special, consequential, or punitive damages arising out of or related to this Agreement, "
        "regardless of the theory of liability. The Provider's total aggregate liability under "
        "this Agreement shall not exceed the total fees paid by the Client during the twelve "
        "(12) month period preceding the claim."
    )

    pdf.section_heading("8", "DISPUTE RESOLUTION")
    pdf.body_text(
        "Any dispute arising out of or relating to this Agreement shall first be submitted "
        "to good-faith mediation. If mediation is unsuccessful, the dispute shall be resolved "
        "through binding arbitration in accordance with the rules of the American Arbitration "
        "Association. The arbitration shall take place in San Francisco, California, and the "
        "decision of the arbitrator shall be final and binding on both Parties."
    )

    pdf.section_heading("9", "GOVERNING LAW")
    pdf.body_text(
        "This Agreement shall be governed by and construed in accordance with the laws of "
        "the State of California, without regard to its conflict of laws provisions."
    )

    pdf.section_heading("10", "ENTIRE AGREEMENT")
    pdf.body_text(
        "This Agreement constitutes the entire understanding between the Parties with respect "
        "to the subject matter hereof and supersedes all prior negotiations, representations, "
        "and agreements, whether written or oral. No modification of this Agreement shall be "
        "effective unless made in writing and signed by both Parties."
    )

    pdf.ln(3)
    pdf.draw_signature_block()

    pdf.output("sample.pdf")
    print("Generated sample.pdf")


if __name__ == "__main__":
    generate()
